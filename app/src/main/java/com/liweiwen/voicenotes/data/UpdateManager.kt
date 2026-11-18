package com.liweiwen.voicenotes.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 内置更新：读取 GitHub Releases 的最新版本，下载 APK 后交给系统安装器。
 * 全部走 IO 线程；失败用 Result 返回，UI 层自行提示，不抛异常到主线程。
 */
object UpdateManager {
    private const val LATEST_API = "https://api.github.com/repos/Mackxin/voice-notes-android/releases/latest"
    const val RELEASES_PAGE = "https://github.com/Mackxin/voice-notes-android/releases"

    data class ReleaseInfo(
        val version: String,
        val apkUrl: String,
        val notes: String,
        val pageUrl: String = RELEASES_PAGE,
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /** 查询最新 Release；无发布或网络失败返回 failure。 */
    suspend fun checkLatest(): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(LATEST_API)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "voice-notes-android")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val json = JSONObject(resp.body?.string().orEmpty())
                val tag = json.optString("tag_name", "").removePrefix("v")
                if (tag.isBlank()) error("没有找到版本信息")
                val notes = json.optString("body", "").ifBlank { "暂无更新说明" }
                var apkUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        if (asset.optString("name", "").endsWith(".apk")) {
                            apkUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }
                ReleaseInfo(tag, apkUrl, notes)
            }
        }
    }

    /** 下载 APK 到私有目录，onProgress 回传 0-100 百分比。 */
    suspend fun download(context: Context, url: String, onProgress: (Int) -> Unit): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (url.isBlank()) error("这个版本没有提供 APK 直链，请到发布页下载")
                val req = Request.Builder().url(url).build()
                val out = File(context.filesDir, "update_latest.apk")
                if (out.exists()) out.delete()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    val body = resp.body ?: error("空响应")
                    val total = body.contentLength()
                    body.byteStream().use { input ->
                        out.outputStream().use { output ->
                            val buf = ByteArray(16 * 1024)
                            var read: Int
                            var done = 0L
                            while (input.read(buf).also { read = it } != -1) {
                                output.write(buf, 0, read)
                                done += read
                                if (total > 0) onProgress((done * 100 / total).toInt())
                            }
                        }
                    }
                }
                out
            }
        }

    /** 用 FileProvider 授权后拉起系统安装界面。 */
    fun install(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** 语义化版本比较：remote 严格大于 local 才返回 true。 */
    fun isNewer(remote: String, local: String): Boolean {
        fun parse(v: String) = v.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val r = parse(remote)
        val l = parse(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv > lv) return true
            if (rv < lv) return false
        }
        return false
    }
}
