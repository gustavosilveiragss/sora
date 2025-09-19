package com.sora.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CollectionModel(
    val id: Long? = null,
    val code: String,
    val nameKey: String,
    val iconName: String? = null,
    val sortOrder: Int = 0,
    val isDefault: Boolean = false
)

@Serializable
enum class CollectionCode {
    GENERAL, CULINARY, EVENTS, OTHERS
}