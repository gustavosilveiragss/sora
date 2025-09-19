package com.sora.android.di

import android.content.Context
import com.sora.android.data.local.SoraDatabase
import com.sora.android.data.local.dao.CountryDao
import com.sora.android.data.local.dao.PostDao
import com.sora.android.data.local.dao.UserDao
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
}