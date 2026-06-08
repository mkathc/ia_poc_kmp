package com.katha.mep.mep_ia_poc.models.chat

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val createdAt: String,
    val isStreaming: Boolean = false,
)

data class ChatRequest(
    val conversationId: String,
    val message: String,
    val context: ConversationContext,
)

data class ConversationContext(
    val userId: String,
    val channel: String,
    val journey: String,
    val locale: String = "es-PE",
)

sealed interface ChatEvent {
    val messageId: String
}

data class MessageStarted(
    override val messageId: String,
) : ChatEvent

data class MessageDelta(
    override val messageId: String,
    val delta: String,
) : ChatEvent

data class MessageCompleted(
    override val messageId: String,
    val text: String,
) : ChatEvent

data class ChatError(
    override val messageId: String,
    val message: String,
    val code: String? = null,
) : ChatEvent

data class HandoffSuggested(
    override val messageId: String,
    val reason: String,
) : ChatEvent
