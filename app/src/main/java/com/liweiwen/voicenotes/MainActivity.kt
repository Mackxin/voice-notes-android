package com.liweiwen.voicenotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.liweiwen.voicenotes.data.NoteStore
import com.liweiwen.voicenotes.data.SettingsStore
import com.liweiwen.voicenotes.ui.NoteScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val noteStore = remember { NoteStore(context.applicationContext) }
            val settingsStore = remember { SettingsStore(context.applicationContext) }
            NoteScreen(noteStore, settingsStore)
        }
    }
}
