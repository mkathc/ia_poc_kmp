package com.katha.mep.mep_ia_poc.data.sdk

import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.contracts.ChatProviderAdapter
import com.katha.mep.mep_ia_poc.contracts.ChatService
import com.katha.mep.mep_ia_poc.models.chat.ChatEvent
import com.katha.mep.mep_ia_poc.models.chat.ChatMessage
import com.katha.mep.mep_ia_poc.models.chat.ChatRequest
import com.katha.mep.mep_ia_poc.models.chat.MessageCompleted
import com.katha.mep.mep_ia_poc.models.chat.MessageDelta
import com.katha.mep.mep_ia_poc.observability.MetricsCollector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlin.time.TimeSource

class NativeSdkChatService(
    private val adapter: ChatProviderAdapter,
    private val configProvider: () -> PocConfig,
    private val metricsCollector: MetricsCollector,
) : ChatService {
    override fun sendMessage(request: ChatRequest): Flow<ChatEvent> {
        val config = configProvider()
        val startMark = TimeSource.Monotonic.markNow()
        var firstTokenRecorded = false
        println("[sdk-adapter] requestStart")
        return adapter.sendMessage(request, config.activeScenario).onEach { event ->
            if (event is MessageDelta && !firstTokenRecorded) {
                firstTokenRecorded = true
                metricsCollector.record(
                    "sdkTimeToFirstToken",
                    mapOf("elapsedMillis" to startMark.elapsedNow().inWholeMilliseconds),
                )
            }
            if (event is MessageCompleted) {
                metricsCollector.record(
                    "sdkTotalResponseTime",
                    mapOf("elapsedMillis" to startMark.elapsedNow().inWholeMilliseconds),
                )
            }
        }
    }

    override suspend fun cancelMessage(messageId: String) = Unit

    override suspend fun getConversation(conversationId: String): List<ChatMessage> = emptyList()
}
