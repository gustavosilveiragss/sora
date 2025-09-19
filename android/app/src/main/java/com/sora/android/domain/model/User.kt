package com.sora.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserModel(
    val id: Long,
    val username: String,
    val email: String? = null,
    val firstName: String,
    val lastName: String,
    val bio: String? = null,
    val profilePicture: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val countriesVisitedCount: Int = 0
)

@Serializable
data class UserProfileModel(
    val id: Long,
    val username: String,
    val email: String? = null,
    val firstName: String,
    val lastName: String,
    val bio: String? = null,
    val profilePicture: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val countriesVisitedCount: Int = 0
)

@Serializable
data class UserSearchResultModel(
    val id: Long,
    val username: String,
    val firstName: String,
    val lastName: String,
    val profilePicture: String? = null
)

@Serializable
data class AuthUserModel(
    val id: Long,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val bio: String? = null,
    val profilePicture: String? = null
)