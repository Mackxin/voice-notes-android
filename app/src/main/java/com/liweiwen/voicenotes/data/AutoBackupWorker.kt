package com.liweiwen.voicenotes.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 定时备份：每天一次，把记录 + 语音打包成 ZIP 写到公开「下载/随手说/自动备份」。
 * 完全离线，不联网、不上传。用户关掉开关时取消任务。
 */
class AutoBackupWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    override fun doWork(): Result {
        val context = applicationContext
        val settings = SettingsStore(context)
        if (!settings.autoBackup) return Result.success()
        return runCatching {
            val notes = NoteStore(context)
            val name = "随手说备份-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())}.zip"
            val uri = createDownloadZipUri(context, name)
                ?: throw IllegalStateException("无法在下载目录创建备份文件")
            BackupManager.export(context, uri, notes, settings)
            settings.lastBackupTime = System.currentTimeMillis()
            Result.success()
        }.getOrElse { Result.failure() }
    }

    companion object {
        private const val WORK_NAME = "voice_notes_auto_backup"

        private fun createDownloadZipUri(context: Context, displayName: String): android.net.Uri? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        "${Environment.DIRECTORY_DOWNLOADS}/随手说/自动备份",
                    )
                }
                context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            } else {
                val dir = java.io.File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "随手说/自动备份",
                ).apply { mkdirs() }
                val file = java.io.File(dir, displayName)
                androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file,
                )
            }
        }

        fun enable(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(true)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun disable(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
