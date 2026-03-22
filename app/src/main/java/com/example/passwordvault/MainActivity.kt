package com.example.passwordvault

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.example.passwordvault.core.ui.theme.PasswordVaultTheme
import com.example.passwordvault.ui.navigation.AppRoot

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )

        val app = application as PasswordVaultApplication
        setContent {
            PasswordVaultTheme {
                AppRoot(container = app.container)
            }
        }
    }
}
