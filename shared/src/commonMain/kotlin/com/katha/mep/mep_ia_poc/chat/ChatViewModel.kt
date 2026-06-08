package com.katha.mep.mep_ia_poc.chat

import com.katha.mep.mep_ia_poc.config.FakeGatewayScenario
import com.katha.mep.mep_ia_poc.config.ProviderMock
import com.katha.mep.mep_ia_poc.contracts.HandoffService
import com.katha.mep.mep_ia_poc.models.chat.ChatMessage
import com.katha.mep.mep_ia_poc.models.chat.ChatRequest
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.resilience.ExperienceContext
import com.katha.mep.mep_ia_poc.models.resilience.ExperienceFallbackType
import com.katha.mep.mep_ia_poc.state.PocDebugState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val appState: StateFlow<PocDebugState>,
    private val changeScenario: (FakeGatewayScenario) -> Unit,
    private val changeProvider: (ProviderMock) -> Unit,
    private val controller: ChatExperienceController,
    private val handoffService: HandoffService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val remoteToLocalAssistantIds = mutableMapOf<String, String>()
    private val mutableState = MutableStateFlow(
        ChatState(
            activeScenario = appState.value.activeScenario,
            activeProvider = appState.value.activeProvider,
        ),
    )
    val state: StateFlow<ChatState> = mutableState.asStateFlow()

    init {
        scope.launch {
            appState.collect { debugState ->
                mutableState.update {
                    it.copy(
                        activeScenario = debugState.activeScenario,
                        activeProvider = debugState.activeProvider,
                    )
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return

        val userMessage = ChatMessage(
            id = "local-user-${state.value.messages.size + 1}",
            text = cleanText,
            isUser = true,
            createdAt = "local-now",
        )

        mutableState.update {
            it.copy(
                messages = it.messages + userMessage,
                isStreaming = true,
                errorMessage = null,
                lastUserMessage = cleanText,
            )
        }

        val request = ChatRequest(
            conversationId = ConversationId,
            message = cleanText,
            context = ConversationContext(
                userId = "poc-user",
                channel = "kmp",
                journey = "chat-streaming-poc",
            ),
        )

        scope.launch {
            controller.sendMessage(request).collect { event ->
                reduce(event)
            }
        }
    }

    fun changeScenario(scenario: FakeGatewayScenario) {
        changeScenario.invoke(scenario)
    }

    fun changeProvider(provider: ProviderMock) {
        changeProvider.invoke(provider)
    }

    fun retryLastMessage() {
        state.value.lastUserMessage?.let(::sendMessage)
    }

    fun dismissFallback() {
        mutableState.update { it.copy(activeFallback = null, hasSlaViolation = false) }
    }

    fun openWhatsappFallback() {
        scope.launch {
            val current = state.value
            val fallback = handoffService.openWhatsappFallback(
                ExperienceContext(
                    feature = "chat",
                    userId = "poc-user",
                    providerId = current.activeProvider.id,
                    scenario = current.activeScenario.wireName,
                ),
            )
            mutableState.update { it.copy(activeFallback = fallback) }
        }
    }

    private fun reduce(event: ChatExperienceEvent) {
        when (event) {
            is ChatExperienceEvent.MessageStarted -> {
                val localMessageId = nextAssistantMessageId(event.messageId)
                remoteToLocalAssistantIds[event.messageId] = localMessageId
                val assistantMessage = ChatMessage(
                    id = localMessageId,
                    text = "",
                    isUser = false,
                    createdAt = "local-now",
                    isStreaming = true,
                )
                mutableState.update { it.copy(messages = it.messages + assistantMessage, isStreaming = true) }
            }
            is ChatExperienceEvent.MessageDelta -> {
                val localMessageId = remoteToLocalAssistantIds.getOrPut(event.messageId) {
                    nextAssistantMessageId(event.messageId)
                }
                mutableState.update { state ->
                    state.copy(
                        messages = state.messages.upsertAssistantMessage(localMessageId) {
                            it.copy(text = event.text, isStreaming = true)
                        },
                        isStreaming = true,
                    )
                }
            }
            is ChatExperienceEvent.MessageCompleted -> {
                val localMessageId = remoteToLocalAssistantIds.getOrPut(event.messageId) {
                    nextAssistantMessageId(event.messageId)
                }
                mutableState.update { state ->
                    state.copy(
                        messages = state.messages.upsertAssistantMessage(localMessageId) {
                            it.copy(text = event.text, isStreaming = false)
                        },
                        isStreaming = false,
                    )
                }
            }
            is ChatExperienceEvent.FallbackTriggered -> {
                println("[ChatViewModel] fallback consolidated type=${event.fallback.type} reason=${event.fallback.reason}")
                mutableState.update {
                    it.copy(
                        activeFallback = event.fallback,
                        hasSlaViolation = event.fallback.type == ExperienceFallbackType.SlaTimeout || it.hasSlaViolation,
                        showEmergencyOptions = event.fallback.type == ExperienceFallbackType.Offline ||
                            event.fallback.type == ExperienceFallbackType.ConnectionLost,
                        errorMessage = null,
                    )
                }
            }
            is ChatExperienceEvent.Error -> {
                println("[ChatViewModel] stream stopped reason=${event.code}")
                mutableState.update { state ->
                    val cleanedMessages = state.messages.stopStreamingAndRemoveEmptyAssistantPlaceholders(event.code)
                    if (cleanedMessages.size != state.messages.size) {
                        println("[ChatViewModel] placeholder removed reason=${event.code}")
                    }
                    val consolidatedError = if (state.activeFallback == null) {
                        event.toUserFacingError()
                    } else {
                        null
                    }
                    state.copy(
                        isStreaming = false,
                        errorMessage = consolidatedError,
                        messages = cleanedMessages,
                    )
                }
            }
        }
    }

    private fun List<ChatMessage>.replaceMessage(
        messageId: String,
        transform: (ChatMessage) -> ChatMessage,
    ): List<ChatMessage> =
        map { message -> if (message.id == messageId) transform(message) else message }

    private fun List<ChatMessage>.upsertAssistantMessage(
        messageId: String,
        transform: (ChatMessage) -> ChatMessage,
    ): List<ChatMessage> {
        val existing = firstOrNull { it.id == messageId }
        return if (existing == null) {
            this + transform(
                ChatMessage(
                    id = messageId,
                    text = "",
                    isUser = false,
                    createdAt = "local-now",
                    isStreaming = true,
                ),
            )
        } else {
            replaceMessage(messageId, transform)
        }
    }

    private fun nextAssistantMessageId(remoteMessageId: String): String =
        "assistant-${state.value.messages.size + 1}-$remoteMessageId"

    private fun List<ChatMessage>.stopStreamingAndRemoveEmptyAssistantPlaceholders(
        errorCode: String?,
    ): List<ChatMessage> =
        mapNotNull { message ->
            when {
                message.isUser -> message
                message.isStreaming && message.text.isBlank() -> null
                message.isStreaming -> message.copy(isStreaming = false)
                else -> message
            }
        }

    private fun ChatExperienceEvent.Error.toUserFacingError(): String =
        when (code) {
            "offline" -> "No tengo conexión con el asistente cloud por el momento."
            "connectionLost" -> "Se perdió la conexión con el asistente. Puedes reintentar."
            else -> "Ocurrió un error con el asistente. Puedes reintentar."
        }

    private companion object {
        const val ConversationId = "poc-chat-conversation"
    }
}
