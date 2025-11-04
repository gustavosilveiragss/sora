package com.sora.android.di

import android.content.Context
import com.sora.android.data.local.SoraDatabase
import com.sora.android.data.local.dao.*
import com.sora.android.debug.DatabaseDebugger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SoraDatabase {
        return SoraDatabase.create(context)
    }

    @Provides
    fun provideUserDao(database: SoraDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun providePostDao(database: SoraDatabase): PostDao {
        return database.postDao()
    }

    @Provides
    fun provideCountryDao(database: SoraDatabase): CountryDao {
        return database.countryDao()
    }

    @Provides
    fun provideCityDao(database: SoraDatabase): CityDao {
        return database.cityDao()
    }

    @Provides
    fun provideCollectionDao(database: SoraDatabase): CollectionDao {
        return database.collectionDao()
    }

    @Provides
    fun provideFollowDao(database: SoraDatabase): FollowDao {
        return database.followDao()
    }

    @Provides
    fun provideLikePostDao(database: SoraDatabase): LikePostDao {
        return database.likePostDao()
    }

    @Provides
    fun provideCommentDao(database: SoraDatabase): CommentDao {
        return database.commentDao()
    }

    @Provides
    fun provideTravelPermissionDao(database: SoraDatabase): TravelPermissionDao {
        return database.travelPermissionDao()
    }

    @Provides
    fun provideNotificationDao(database: SoraDatabase): NotificationDao {
        return database.notificationDao()
    }

    @Provides
    fun providePostMediaDao(database: SoraDatabase): PostMediaDao {
        return database.postMediaDao()
    }

    @Provides
    fun provideDraftPostDao(database: SoraDatabase): DraftPostDao {
        return database.draftPostDao()
    }

    @Provides
    fun provideUserStatsDao(database: SoraDatabase): UserStatsDao {
        return database.userStatsDao()
    }

    @Provides
    fun provideGlobeDao(database: SoraDatabase): GlobeDao {
        return database.globeDao()
    }

    @Provides
    @Singleton
    fun provideDatabaseDebugger(database: SoraDatabase): DatabaseDebugger {
        return DatabaseDebugger(database)
    }
}