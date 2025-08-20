package com.sora.android.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Message(
    val id: Long? = null,
    val content: String,
    val createdAt: String? = null
)