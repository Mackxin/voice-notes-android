package com.liweiwen.voicenotes.asr

import com.liweiwen.voicenotes.audio.PcmRecorder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed interface AsrResult {
    data class Success(val text: String) : AsrResult
    data class Failure(val message: String) : AsrResult
}

/**
 * 百炼 paraformer-realtime-v2 双向流式 WebSocket 客户端。
 * 流程：connect() -> 不停 sendAudio() -> finish()，全过程可收到中间结果。
 */
class DashScopeAsr(
    private val apiKey: String,
    private val wsUrl: String,
    private val model: String,
    private val onPartial: (String) -> Unit,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private val taskId: String = UUID.randomUUID().toString()
    private val started = CompletableDeferred<Boolean>()
    private val completed = CompletableDeferred<AsrResult>()
    private val finalParts = mutableListOf<String>()
    private var interim = ""
    private var lastFinal = ""
    private var socket: WebSocket? = null

    suspend fun connect(): Boolean {
        val request = Request.Builder()
            .url(wsUrl)
            .header("Authorization", "Bearer $apiKey")
            .build()

        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val runTask = JSONObject()
                    .put("header", JSONObject()
                        .put("action", "run-task")
                        .put("task_id", taskId)
                        .put("streaming", "duplex"))
                    .put("payload", JSONObject()
                        .put("task_group", "audio")
                        .put("task", "asr")
                        .put("function", "recognition")
                        .put("model", model)
                        .put("input", JSONObject())
                        .put("parameters", JSONObject()
                            .put("format", "pcm")
                            .put("sample_rate", PcmRecorder.SAMPLE_RATE)
                            .put("language_hints", org.json.JSONArray().put("zh"))
                            .put("punctuation_prediction_enabled", true)
                            .put("inverse_text_normalization_enabled", true)))
                webSocket.send(runTask.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val message = t.message?.takeIf { it.isNotBlank() } ?: "网络连接失败"
                started.complete(false)
                completed.complete(AsrResult.Failure(message))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                completed.complete(AsrResult.Failure("连接已关闭：$reason"))
            }
        })

        return withTimeout(CONNECT_TIMEOUT_MS) { started.await() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun sendAudio(data: ByteArray) {
        if (started.isCompleted && started.getCompleted()) {
            socket?.send(ByteString.of(*data))
        }
    }

    suspend fun finish(): AsrResult {
        val finishTask = JSONObject()
            .put("header", JSONObject()
                .put("action", "finish-task")
                .put("task_id", taskId)
                .put("streaming", "duplex"))
            .put("payload", JSONObject().put("input", JSONObject()))
        socket?.send(finishTask.toString())

        return withTimeout(FINISH_TIMEOUT_MS) { completed.await() }
    }

    fun cancel() {
        try {
            socket?.close(1000, "cancelled")
        } catch (_: Exception) {
            // 忽略关闭异常
        }
        socket = null
    }

    private fun handleMessage(text: String) {
        try {
            val root = JSONObject(text)
            val event = root.optJSONObject("header")?.optString("event").orEmpty()
            when (event) {
                "task-started" -> started.complete(true)

                "result-generated" -> {
                    val sentence = root.optJSONObject("payload")
                        ?.optJSONObject("output")
                        ?.optJSONObject("sentence") ?: return
                    if (sentence.optBoolean("heartbeat")) return

                    val content = sentence.optString("text").orEmpty()
                    if (sentence.optBoolean("sentence_end")) {
                        if (content.isNotBlank() && content != lastFinal) {
                            finalParts.add(content)
                            lastFinal = content
                        }
                        interim = ""
                    } else {
                        interim = content
                    }
                    onPartial((finalParts + interim).joinToString(""))
                }

                "task-finished" -> {
                    completed.complete(AsrResult.Success(finalParts.joinToString("").trim()))
                }

                "task-failed" -> {
                    val header = root.optJSONObject("header")
                    val message = header?.optString("error_message").orEmpty()
                        .ifBlank { "语音识别失败" }
                    completed.complete(AsrResult.Failure(message))
                }
            }
        } catch (_: Exception) {
            // 忽略无法解析的服务端消息
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 12_000L
        private const val FINISH_TIMEOUT_MS = 20_000L
    }
}
