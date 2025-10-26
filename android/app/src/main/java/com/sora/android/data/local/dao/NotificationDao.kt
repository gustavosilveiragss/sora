package com.sora.android.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.sora.android.data.local.entity.Notification
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notification WHERE recipientId = :userId ORDER BY createdAt DESC")
    fun getUserNotifications(userId: Long): PagingSource<Int, Notification>

    @Query("SELECT * FROM notification WHERE recipientId = :userId AND isRead = 0 ORDER BY createdAt DESC")
    fun getUnreadNotifications(userId: Long): PagingSource<Int, Notification>

    @Query("SELECT * FROM notification WHERE recipientId = :userId AND type = :type ORDER BY createdAt DESC")
    fun getNotificationsByType(userId: Long, type: String): PagingSource<Int, Notification>

    @Query("SELECT * FROM notification WHERE id = :notificationId")
    suspend fun getNotificationById(notificationId: Long): Notification?

    @Query("SELECT * FROM notification WHERE id = :notificationId")
    fun getNotificationByIdFlow(notificationId: Long): Flow<Notification?>

    @Query("SELECT COUNT(*) FROM notification WHERE recipientId = :userId AND isRead = 0")
    suspend fun getUnreadCount(userId: Long): Int

    @Query("SELECT COUNT(*) FROM notification WHERE recipientId = :userId AND isRead = 0")
    fun getUnreadCountFlow(userId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM notification WHERE recipientId = :userId")
    suspend fun getTotalNotificationsCount(userId: Long): Int

    @Query("SELECT COUNT(*) FROM notification WHERE recipientId = :userId AND type = :type")
    suspend fun getNotificationsCountByType(userId: Long, type: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<Notification>)

    @Update
    suspend fun updateNotification(notification: Notification)

    @Delete
    suspend fun deleteNotification(notification: Notification)

    @Query("DELETE FROM notification WHERE id = :notificationId")
    suspend fun deleteNotificationById(notificationId: Long)

    @Query("UPDATE notification SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Long)

    @Query("UPDATE notification SET isRead = 1 WHERE recipientId = :userId AND isRead = 0")
    suspend fun markAllAsRead(userId: Long): Int

    @Query("UPDATE notification SET isRead = 1 WHERE recipientId = :userId AND type = :type AND isRead = 0")
    suspend fun markAllAsReadByType(userId: Long, type: String): Int

    @Query("DELETE FROM notification WHERE recipientId = :userId")
    suspend fun deleteAllUserNotifications(userId: Long)

    @Query("DELETE FROM notification WHERE cacheTimestamp < :expiry")
    suspend fun deleteExpiredNotifications(expiry: Long): Int

    @Query("UPDATE notification SET cacheTimestamp = :timestamp WHERE id = :notificationId")
    suspend fun touchNotificationCache(notificationId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM notification")
    suspend fun getNotificationCount(): Int
}