package com.katha.mep.mep_ia_poc.contracts

import com.katha.mep.mep_ia_poc.models.chat.ChatEvent
import com.katha.mep.mep_ia_poc.models.chat.ChatMessage
import com.katha.mep.mep_ia_poc.models.chat.ChatRequest
import kotlinx.coroutines.flow.Flow

interface ChatService {
    fun sendMessage(request: ChatRequest): Flow<ChatEvent>
    suspend fun cancelMessage(messageId: String)
    suspend fun getConversation(conversationId: String): List<ChatMessage>
}
