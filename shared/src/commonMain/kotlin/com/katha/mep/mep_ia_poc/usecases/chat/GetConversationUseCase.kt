package com.katha.mep.mep_ia_poc.usecases.chat

import com.katha.mep.mep_ia_poc.contracts.ChatService
import com.katha.mep.mep_ia_poc.models.chat.ChatMessage

class GetConversationUseCase(
    private val chatService: ChatService,
) {
    suspend operator fun invoke(conversationId: String): List<ChatMessage> =
        chatService.getConversation(conversationId)
}
