package com.sora.android.core.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.sora.android.R
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun formatTimeAgo(timestamp: String): String {
    val result = remember(timestamp) {
        calculateTimeAgo(timestamp)
    }

    return when {
        result == null -> timestamp
        result.seconds < 60 -> stringResource(R.string.just_now)
        result.minutes < 2 -> stringResource(R.string.one_minute)
        result.minutes < 60 -> stringResource(R.string.minutes, result.minutes.toInt())
        result.hours < 2 -> stringResource(R.string.one_hour)
        result.hours < 24 -> stringResource(R.string.hours, result.hours.toInt())
        result.days < 2 -> stringResource(R.string.one_day)
        result.days < 7 -> stringResource(R.string.days, result.days.toInt())
        result.weeks < 2 -> stringResource(R.string.one_week)
        result.weeks < 4 -> stringResource(R.string.weeks, result.weeks.toInt())
        result.months < 2 -> stringResource(R.string.one_month)
        else -> stringResource(R.string.months, result.months.toInt())
    }
}

private data class TimeAgoResult(
    val seconds: Long,
    val minutes: Long,
    val hours: Long,
    val days: Long,
    val weeks: Long,
    val months: Long
)

private fun calculateTimeAgo(timestamp: String): TimeAgoResult? {
    return try {
        val now = System.currentTimeMillis()
        val time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            .parse(timestamp)?.time ?: return null

        val diff = now - time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = days / 30

        TimeAgoResult(seconds, minutes, hours, days, weeks, months)
    } catch (e: Exception) {
        null
    }
}
