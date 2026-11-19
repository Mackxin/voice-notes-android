package com.liweiwen.voicenotes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.liweiwen.voicenotes.MainActivity
import com.liweiwen.voicenotes.R
import com.liweiwen.voicenotes.data.AppBus
import com.liweiwen.voicenotes.data.NoteStore

/**
 * 桌面小组件：显示最近两条记录，点标题打开 App，点「录音」直接开始录音。
 * 录音按钮通过打开 MainActivity 并带 extra，由 MainActivity 转发 AppBus 事件。
 */
class NotesWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> updateOne(context, manager, id) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // 录音按钮的 PendingIntent 直接发广播到这里，转发事件（不强制拉起界面）
        if (intent.action == ACTION_RECORD) {
            AppBus.emit(AppBus.START_RECORD)
        }
    }

    companion object {
        const val ACTION_RECORD = "com.liweiwen.voicenotes.widget.RECORD"
        const val EXTRA_START_RECORD = "start_record"

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NotesWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                NotesWidgetProvider().onUpdate(context, manager, ids)
            }
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val notes = NoteStore(context.applicationContext).all().take(2)

            if (notes.isEmpty()) {
                views.setViewVisibility(R.id.widget_note1, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_note2, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_empty, android.view.View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_empty, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_note1, android.view.View.VISIBLE)
                views.setTextViewText(R.id.widget_note1, notes[0].text.take(40))
                if (notes.size > 1) {
                    views.setViewVisibility(R.id.widget_note2, android.view.View.VISIBLE)
                    views.setTextViewText(R.id.widget_note2, notes[1].text.take(40))
                } else {
                    views.setViewVisibility(R.id.widget_note2, android.view.View.GONE)
                }
            }

            val openFlag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val openIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(context, 1, openIntent, openFlag),
            )

            // 直接打开 MainActivity 并带 extra，保证进程被杀后也能触发录音
            val recordOpen = Intent(context, MainActivity::class.java).apply {
                action = ACTION_RECORD
                putExtra(EXTRA_START_RECORD, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            views.setOnClickPendingIntent(
                R.id.widget_record,
                PendingIntent.getActivity(context, 2, recordOpen, openFlag),
            )

            manager.updateAppWidget(id, views)
        }
    }
}
