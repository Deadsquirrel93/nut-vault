package com.example.passwordvault.core.ui

import android.content.Context
import androidx.annotation.StringRes

sealed interface UiText {
    fun resolve(context: Context): String

    data class DynamicString(val value: String) : UiText {
        override fun resolve(context: Context): String = value
    }

    data class StringResource(
        @StringRes val resId: Int,
        val args: List<Any> = emptyList(),
    ) : UiText {
        override fun resolve(context: Context): String = context.getString(resId, *args.toTypedArray())
    }
}
