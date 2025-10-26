package com.sora.android.di

import android.content.Context
import com.sora.android.BuildConfig
import com.sora.android.core.config.ApiConfig
import com.sora.android.data.local.TokenManager
import com.sora.android.data.remote.ApiService
import com.sora.android.data.remote.interceptor.AuthInterceptor
import com.sora.android.data.remote.interceptor.TokenRefreshInterceptor
import com.sora.android.core.error.NetworkErrorInterceptor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import javax.inject.Named
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    
    companion object {
        @Provides
        @Singleton
        fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
            return TokenManager(context)
        }

        @Provides
        @Singleton
        fun provideJson(): Json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        @Provides
        @Singleton
        @Named("refresh")
        fun provideRefreshOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = if (BuildConfig.DEBUG) {
                            HttpLoggingInterceptor.Level.BODY
                        } else {
                            HttpLoggingInterceptor.Level.NONE
                        }
                    }
                )
                .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .build()
        }

        @Provides
        @Singleton
        fun provideOkHttpClient(
            authInterceptor: AuthInterceptor,
            tokenRefreshInterceptor: TokenRefreshInterceptor,
            networkErrorInterceptor: NetworkErrorInterceptor
        ): OkHttpClient {
            return OkHttpClient.Builder()
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = if (BuildConfig.DEBUG) {
                            HttpLoggingInterceptor.Level.BODY
                        } else {
                            HttpLoggingInterceptor.Level.NONE
                        }
                    }
                )
                .addInterceptor(networkErrorInterceptor)
                .addInterceptor(authInterceptor)
                .addInterceptor(tokenRefreshInterceptor)
                .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .build()
        }
        
        @Provides
        @Singleton
        fun provideRetrofit(
            okHttpClient: OkHttpClient,
            json: Json
        ): Retrofit {
            return Retrofit.Builder()
                .baseUrl(ApiConfig.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(
                    json.asConverterFactory("application/json".toMediaType())
                )
                .build()
        }
        
        @Provides
        @Singleton
        @Named("refresh")
        fun provideRefreshRetrofit(
            @Named("refresh") refreshOkHttpClient: OkHttpClient,
            json: Json
        ): Retrofit {
            return Retrofit.Builder()
                .baseUrl(ApiConfig.BASE_URL)
                .client(refreshOkHttpClient)
                .addConverterFactory(
                    json.asConverterFactory("application/json".toMediaType())
                )
                .build()
        }

        @Provides
        @Singleton
        @Named("refresh")
        fun provideRefreshApiService(@Named("refresh") refreshRetrofit: Retrofit): ApiService {
            return refreshRetrofit.create(ApiService::class.java)
        }

        @Provides
        @Singleton
        fun provideApiService(retrofit: Retrofit): ApiService {
            return retrofit.create(ApiService::class.java)
        }
    }
}