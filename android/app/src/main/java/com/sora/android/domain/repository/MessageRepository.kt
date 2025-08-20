package com.sora.android.domain.repository

import com.sora.android.domain.model.Message

interface MessageRepository {
    suspend fun getHelloWorld(): Result<Message>
}