package com.sora.android.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import androidx.annotation.WorkerThread
import com.sora.android.data.local.converter.StringListConverter
import com.sora.android.data.local.dao.*
import com.sora.android.data.local.entity.*

@Database(
    entities = [
        User::class,
        Post::class,
        Country::class,
        CountryCollection::class,
        City::class,
        DraftPost::class,
        PostCollection::class,
        Follow::class,
        LikePost::class,
        Comment::class,
        TravelPermission::class,
        Notification::class,
        PostMedia::class,
        CachedUserStats::class,
        ProfileGlobeDataEntity::class,
        ProfileCountryMarkerEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(StringListConverter::class)
abstract class SoraDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun countryDao(): CountryDao
    abstract fun cityDao(): CityDao
    abstract fun collectionDao(): CollectionDao
    abstract fun followDao(): FollowDao
    abstract fun likePostDao(): LikePostDao
    abstract fun commentDao(): CommentDao
    abstract fun travelPermissionDao(): TravelPermissionDao
    abstract fun notificationDao(): NotificationDao
    abstract fun postMediaDao(): PostMediaDao
    abstract fun draftPostDao(): DraftPostDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun globeDao(): GlobeDao

    companion object {
        const val DATABASE_NAME = "sora_database"

        fun create(context: Context): SoraDatabase {
            return Room.databaseBuilder(
                context,
                SoraDatabase::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}