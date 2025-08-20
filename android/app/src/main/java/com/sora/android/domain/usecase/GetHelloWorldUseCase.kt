package com.sora.android.domain.usecase

import com.sora.android.domain.model.Message
import com.sora.android.domain.repository.MessageRepository
import javax.inject.Inject

class GetHelloWorldUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(): Result<Message> {
        return messageRepository.getHelloWorld()
    }
}