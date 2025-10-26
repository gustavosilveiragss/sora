package com.sora.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "travel_permission",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["grantorId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["granteeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Country::class,
            parentColumns = ["id"],
            childColumns = ["countryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["grantorId", "granteeId", "countryId"], unique = true),
        Index(value = ["grantorId"]),
        Index(value = ["granteeId"]),
        Index(value = ["countryId"])
    ]
)
data class TravelPermission(
    @PrimaryKey val id: Long,
    val grantorId: Long, // Who grants permission
    val grantorUsername: String,
    val granteeId: Long, // Who receives permission
    val granteeUsername: String,
    val countryId: Long,
    val countryCode: String,
    val countryNameKey: String,
    val status: String, // PENDING, ACTIVE, DECLINED, REVOKED
    val invitationMessage: String? = null,
    val respondedAt: String? = null,
    val createdAt: String,
    val cacheTimestamp: Long = System.currentTimeMillis()
)