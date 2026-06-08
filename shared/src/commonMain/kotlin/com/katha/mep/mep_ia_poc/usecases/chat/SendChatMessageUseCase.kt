package com.katha.mep.mep_ia_poc.usecases.chat

import com.katha.mep.mep_ia_poc.contracts.ChatService
import com.katha.mep.mep_ia_poc.models.chat.ChatEvent
import com.katha.mep.mep_ia_poc.models.chat.ChatRequest
import kotlinx.coroutines.flow.Flow

class SendChatMessageUseCase(
    private val chatService: ChatService,
) {
    operator fun invoke(request: ChatRequest): Flow<ChatEvent> =
        chatService.sendMessage(request)
}
