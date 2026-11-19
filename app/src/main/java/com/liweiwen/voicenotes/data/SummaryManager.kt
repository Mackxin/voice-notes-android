package com.liweiwen.voicenotes.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * AI 自动摘要：复用 DashScope 的 OpenAI 兼容对话接口，让模型从一条转写文字里提取要点。
 * 只把用户这一条记录发给百炼，其余记录不上传。
 */
object SummaryManager {
    private const val URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"

    sealed interface Result {
        data class Success(val text: String) : Result
        data class Failure(val message: String) : Result
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun summarize(
        apiKey: String,
        model: String,
        text: String,
    ): Result = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.Failure("请先在设置里填写 API Key")
        if (text.isBlank() || text == "[语音消息]") return@withContext Result.Failure("这条记录没有可摘要的文字")

        val prompt = "请把下面这段速记提炼成一条简洁的中文摘要（不超过40字），只输出摘要本身，不要解释、不要引号：\n\n$text"
        val messages = JSONArray().put(
            JSONObject().put("role", "user").put("content", prompt),
        )
        val body = JSONObject()
            .put("model", model.ifBlank { "qwen-plus" })
            .put("messages", messages)
            .put("max_tokens", 200)
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(URL)
            .header("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val msg = runCatching {
                        JSONObject(raw).optJSONObject("error")?.optString("message")
                    }.getOrNull()
                    return@withContext Result.Failure(msg?.ifBlank { null } ?: "服务返回 ${response.code}")
                }
                val answer = JSONObject(raw)
                    .optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    .orEmpty()
                    .trim()
                if (answer.isBlank()) Result.Failure("摘要为空，请重试") else Result.Success(answer)
            }
        }.getOrElse { Result.Failure(it.message ?: "摘要失败，请检查网络") }
    }
}
