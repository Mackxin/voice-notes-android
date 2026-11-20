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

    /** 摘要 + 自动标签的结果。 */
    data class Rich(val summary: String, val tags: List<String>)

    sealed interface Result2 {
        data class Ok(val rich: Rich) : Result2
        data class Err(val message: String) : Result2
    }

    /**
     * 摘要时顺带让模型给出 1-3 个标签。
     * 有真实 Key 时请求百炼返回 JSON；否则本机降级（截取首句 + 从已有标签里挑命中的词）。
     */
    suspend fun rich(apiKey: String, model: String, text: String, knownTags: List<String>): Result2 = withContext(Dispatchers.IO) {
        if (text.isBlank() || text == "[语音消息]") return@withContext Result2.Err("这条记录没有可摘要的文字")
        val keyOk = apiKey.startsWith("sk-") && apiKey.length > 20
        if (keyOk) {
            val json = runCatching { askJson(apiKey, model, text) }.getOrNull()
            if (json != null) return@withContext Result2.Ok(json)
        }
        // 本地降级
        val local = localSummary(text)
        val picked = knownTags.filter { text.contains(it, ignoreCase = true) }.take(3)
        Result2.Ok(Rich(local, picked))
    }

    private fun localSummary(text: String): String {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        val firstSentence = clean.split(Regex("[。！？!?；;\n]")).firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        val src = firstSentence.ifBlank { clean }
        return if (src.length <= 42) src else src.take(42) + "…"
    }

    private fun extractTags(apiKey: String, model: String, text: String): List<String> {
        val prompt = "为这段速记提炼1到3个中文标签（每个不超过6个字），只输出用英文逗号分隔的标签，不要解释：\n\n$text"
        val answer = chat(apiKey, model, prompt, 80) ?: return emptyList()
        return answer.split(Regex("[,，、\\s]+")).map { it.trim().trim('#') }.filter { it.isNotBlank() && it.length <= 8 }.distinct().take(3)
    }

    private fun askJson(apiKey: String, model: String, text: String): Rich? {
        val prompt = "处理这段速记，输出严格 JSON：{\"summary\":\"不超过40字的中文摘要\",\"tags\":[\"标签1\",\"标签2\"]}，标签1到3个，只输出 JSON：\n\n$text"
        val answer = chat(apiKey, model, prompt, 260) ?: return null
        val obj = runCatching { JSONObject(answer.trim().removePrefix("```json").removeSuffix("```").trim()) }.getOrNull() ?: return null
        val s = obj.optString("summary").ifBlank { localSummary(text) }
        val tags = obj.optJSONArray("tags")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { t -> t.isNotBlank() && t.length <= 8 } }.distinct().take(3)
        }.orEmpty()
        return Rich(s, tags)
    }

    private fun chat(apiKey: String, model: String, prompt: String, maxTokens: Int): String? {
        val messages = JSONArray().put(JSONObject().put("role", "user").put("content", prompt))
        val body = JSONObject()
            .put("model", model.ifBlank { "qwen-plus" })
            .put("messages", messages)
            .put("max_tokens", maxTokens)
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(URL).header("Authorization", "Bearer $apiKey").post(body).build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val raw = response.body?.string().orEmpty()
            JSONObject(raw).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")?.trim()
        }
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
