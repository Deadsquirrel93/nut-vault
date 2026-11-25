package com.example.passwordvault.core.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ClipboardHelper(
    context: Context,
    private val applicationScope: CoroutineScope,
) {
    private val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var clearJob: Job? = null

    fun copy(
        label: String,
        value: String,
        isSensitive: Boolean = false,
        clearAfterMillis: Long? = if (isSensitive) DEFAULT_CLEAR_DELAY_MS else null,
    ) {
        val clip = ClipData.newPlainText(label, value).apply {
            if (isSensitive) {
                description.extras = PersistableBundle().apply {
                    val key = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ClipDescription.EXTRA_IS_SENSITIVE
                    } else {
                        "android.content.extra.IS_SENSITIVE"
                    }
                    putBoolean(key, true)
                }
            }
        }
        clipboardManager.setPrimaryClip(clip)

        clearJob?.cancel()
        if (clearAfterMillis != null) {
            clearJob = applicationScope.launch {
                delay(clearAfterMillis)
                val currentText = runCatching {
                    clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
                }.getOrNull()
                if (currentText == value) {
                    runCatching { clipboardManager.clearPrimaryClip() }
                        .onFailure { clipboardManager.setPrimaryClip(ClipData.newPlainText("", "")) }
                }
            }
        }
    }

    companion object {
        const val DEFAULT_CLEAR_DELAY_MS = 30_000L
    }
}
