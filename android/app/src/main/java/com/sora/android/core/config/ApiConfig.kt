package com.sora.android.core.config

import com.sora.android.BuildConfig

object ApiConfig {
    val BASE_URL: String = BuildConfig.BASE_URL

    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}