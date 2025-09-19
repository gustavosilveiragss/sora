package com.sora.android.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
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
        DraftPost::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(StringListConverter::class)
abstract class SoraDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun countryDao(): CountryDao

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