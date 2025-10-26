package com.sora.android.core.translation

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun translateAny(key: String, fallback: String): String =
        context.resources.getIdentifier(
            key.replace(".", "_"),
            "string",
            context.packageName
        ).takeIf { it != 0 }?.let { context.getString(it) } ?: fallback
}