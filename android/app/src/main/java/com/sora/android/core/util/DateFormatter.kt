package com.sora.android.core.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatter {
    private val ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private val DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
    private val DISPLAY_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun formatApiDateToDisplay(apiDateString: String?): String {
        return try {
            if (apiDateString.isNullOrBlank()) {
                ""
            } else {
                val localDateTime = LocalDateTime.parse(apiDateString, ISO_DATE_TIME_FORMATTER)
                localDateTime.format(DISPLAY_DATE_FORMATTER)
            }
        } catch (e: Exception) {
            apiDateString ?: ""
        }
    }

    fun formatApiDateTimeToDisplay(apiDateString: String?): String {
        return try {
            if (apiDateString.isNullOrBlank()) {
                ""
            } else {
                val localDateTime = LocalDateTime.parse(apiDateString, ISO_DATE_TIME_FORMATTER)
                localDateTime.format(DISPLAY_DATE_TIME_FORMATTER)
            }
        } catch (e: Exception) {
            apiDateString ?: ""
        }
    }
}