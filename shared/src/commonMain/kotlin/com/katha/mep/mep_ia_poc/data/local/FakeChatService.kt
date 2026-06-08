package com.katha.mep.mep_ia_poc.data.local

import com.katha.mep.mep_ia_poc.config.FakeGatewayScenario
import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.contracts.ChatService
import com.katha.mep.mep_ia_poc.models.chat.ChatEvent
import com.katha.mep.mep_ia_poc.models.chat.ChatError
import com.katha.mep.mep_ia_poc.models.chat.ChatMessage
import com.katha.mep.mep_ia_poc.models.chat.ChatRequest
import com.katha.mep.mep_ia_poc.models.chat.MessageCompleted
import com.katha.mep.mep_ia_poc.models.chat.MessageDelta
import com.katha.mep.mep_ia_poc.models.chat.MessageStarted
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeChatService(
    private val configProvider: () -> PocConfig = { PocConfig() },
) : ChatService {
    private val messages = mutableListOf<ChatMessage>()
    private var requestCount = 0

    override fun sendMessage(request: ChatRequest): Flow<ChatEvent> = flow {
        val config = configProvider()
        requestCount += 1
        val messageId = "fake-${request.conversationId}-$requestCount"
        val providerPrefix = "[${config.provider.id}] "

        if (config.activeScenario == FakeGatewayScenario.offline) {
            emit(ChatError(messageId, "Offline controlled scenario", "offline"))
            return@flow
        }

        emit(MessageStarted(messageId))

        when (config.activeScenario) {
            FakeGatewayScenario.error -> emit(ChatError(messageId, "Controlled error", "fake_error"))
            FakeGatewayScenario.connectionLost -> {
                emit(MessageDelta(messageId, providerPrefix + "Respuesta parcial antes del corte. "))
                emit(ChatError(messageId, "Connection lost before completion", "connectionLost"))
            }
            FakeGatewayScenario.longStream -> {
                val chunks = (1..80).map { "token-$it " }
                chunks.forEach { chunk ->
                    delay(40)
                    emit(MessageDelta(messageId, chunk))
                }
                emit(MessageCompleted(messageId, chunks.joinToString(separator = "")))
            }
            FakeGatewayScenario.timeout -> {
                delay(config.slaThresholdMillis + 700)
                val text = providerPrefix + "Respuesta tardia despues del SLA."
                emit(MessageDelta(messageId, text))
                emit(MessageCompleted(messageId, text))
            }
            FakeGatewayScenario.delayed -> {
                delay(1_200)
                val text = providerPrefix + "Respuesta con latencia simulada."
                emit(MessageDelta(messageId, text))
                emit(MessageCompleted(messageId, text))
            }
            else -> {
                val chunks = listOf(providerPrefix, "Respuesta ", "streaming ", "lista.")
                chunks.forEach { chunk ->
                    delay(120)
                    emit(MessageDelta(messageId, chunk))
                }
                emit(MessageCompleted(messageId, chunks.joinToString(separator = "")))
            }
        }
    }

    override suspend fun cancelMessage(messageId: String) = Unit

    override suspend fun getConversation(conversationId: String): List<ChatMessage> =
        messages.toList()
}
