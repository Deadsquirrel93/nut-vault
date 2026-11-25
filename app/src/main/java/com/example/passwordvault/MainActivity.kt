package com.example.passwordvault

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.core.ui.theme.PasswordVaultTheme
import com.example.passwordvault.core.ui.theme.shouldUseDarkTheme
import com.example.passwordvault.ui.navigation.AppRoot

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )

        val app = application as PasswordVaultApplication
        setContent {
            val themeMode by app.container.appPreferences.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by app.container.appPreferences.dynamicColor.collectAsStateWithLifecycle()
            val darkTheme = shouldUseDarkTheme(themeMode)

            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                    ) { darkTheme },
                )
                onDispose {}
            }

            PasswordVaultTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                AppRoot(container = app.container)
            }
        }
    }
}
