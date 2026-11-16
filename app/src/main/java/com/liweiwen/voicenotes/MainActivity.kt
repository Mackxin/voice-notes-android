package com.liweiwen.voicenotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.liweiwen.voicenotes.data.NoteStore
import com.liweiwen.voicenotes.data.SettingsStore
import com.liweiwen.voicenotes.ui.NoteScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF07C160),
                    background = Color(0xFFF5F5F5),
                ),
            ) {
                val context = LocalContext.current
                val noteStore = remember { NoteStore(context.applicationContext) }
                val settingsStore = remember { SettingsStore(context.applicationContext) }
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F5F5)) {
                    NoteScreen(noteStore, settingsStore)
                }
            }
        }
    }
}
