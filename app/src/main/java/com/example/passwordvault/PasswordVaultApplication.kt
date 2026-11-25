package com.example.passwordvault

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.passwordvault.app.AppContainer
import kotlinx.coroutines.launch

class PasswordVaultApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    container.applicationScope.launch {
                        if (container.authRepository.getSettings().protectionEnabled) {
                            val timeoutMs = container.appPreferences.autoLockTimeout.value.durationMillis
                            container.sessionManager.onAppBackgrounded(timeoutMs, container.applicationScope)
                        }
                    }
                }

                override fun onStart(owner: LifecycleOwner) {
                    val timeoutMs = container.appPreferences.autoLockTimeout.value.durationMillis
                    container.sessionManager.onAppForegrounded(timeoutMs)
                }
            },
        )
    }
}
