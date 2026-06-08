package com.katha.mep.mep_ia_poc.data

import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.contracts.ChatService
import com.katha.mep.mep_ia_poc.models.chat.ChatEvent
import com.katha.mep.mep_ia_poc.models.chat.ChatMessage
import com.katha.mep.mep_ia_poc.models.chat.ChatRequest
import kotlinx.coroutines.flow.Flow

class ChatServiceRouter(
    private val configProvider: () -> PocConfig,
    private val remoteChatService: ChatService,
    private val fakeChatService: ChatService,
) : ChatService {
    override fun sendMessage(request: ChatRequest): Flow<ChatEvent> =
        activeService().sendMessage(request)

    override suspend fun cancelMessage(messageId: String) {
        activeService().cancelMessage(messageId)
    }

    override suspend fun getConversation(conversationId: String): List<ChatMessage> =
        activeService().getConversation(conversationId)

    private fun activeService(): ChatService =
        if (configProvider().useRemoteGateway) remoteChatService else fakeChatService
}
