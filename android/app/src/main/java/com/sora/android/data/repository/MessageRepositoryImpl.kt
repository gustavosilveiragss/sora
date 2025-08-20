package com.sora.android.data.repository

import com.sora.android.data.remote.ApiService
import com.sora.android.domain.model.Message
import com.sora.android.domain.repository.MessageRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : MessageRepository {
    
    override suspend fun getHelloWorld(): Result<Message> {
        return try {
            val message = apiService.getHelloWorld()
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}