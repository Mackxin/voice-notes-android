package com.liweiwen.voicenotes

import android.app.Activity
import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * 透明的生物识别验证页：只负责调系统指纹/面容弹窗，成功返回 RESULT_OK。
 * 独立 Activity 避免 BiometricPrompt 要求 FragmentActivity 与 Compose 权限 launcher 冲突。
 */
class BiometricActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                setResult(Activity.RESULT_OK)
                finish()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
            override fun onAuthenticationFailed() {
                // 单次失败不关闭弹窗，让用户继续尝试
            }
        }
        BiometricPrompt(this, executor, callback).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("解锁随手说")
                .setSubtitle("验证指纹或面容")
                .setNegativeButtonText("取消")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build(),
        )
    }
}
