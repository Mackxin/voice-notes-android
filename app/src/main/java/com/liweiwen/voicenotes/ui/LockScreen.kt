package com.liweiwen.voicenotes.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liweiwen.voicenotes.BiometricActivity
import com.liweiwen.voicenotes.data.SettingsStore

/**
 * 隐私锁屏：可用系统指纹/面容，或 4-12 位密码解锁。
 * 验证逻辑全部在本机完成（密码存 SHA-256），不联网。
 * 生物识别走独立透明 Activity，避免与 Compose 权限请求的 requestCode 冲突。
 */
@Composable
fun LockScreen(settings: SettingsStore, onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val accent = Color(0xFF00A88C)
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    val biometricLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            onUnlocked()
        } else if (result.resultCode == android.app.Activity.RESULT_CANCELED) {
            error = "验证未通过，请用密码"
        }
    }

    fun unlockByPin() {
        if (SettingsStore.sha256(pin) == settings.lockPinHash) {
            onUnlocked()
        } else {
            error = "密码不正确"
            pin = ""
        }
    }

    fun showBiometric() {
        val canAuth = BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG,
        ) == BiometricManager.BIOMETRIC_SUCCESS
        if (!canAuth) {
            error = "本机未录入指纹/面容，请用密码"
            return
        }
        biometricLauncher.launch(Intent(context, BiometricActivity::class.java))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1020)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF00A88C), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcons.Lock,
                    contentDescription = "已锁定",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("随手说已锁定", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text("输入密码或验证指纹继续", fontSize = 13.sp, color = Color(0xFF9FB3B0))

            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it; error = "" },
                label = { Text("密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = error.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (error.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(error, color = Color(0xFFE64340), fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { unlockByPin() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
            ) {
                Text("解锁", color = Color.White, fontSize = 16.sp)
            }
            if (settings.lockBiometric) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { showBiometric() }) {
                    Text("使用指纹/面容解锁", color = accent)
                }
            }
        }
    }
}
