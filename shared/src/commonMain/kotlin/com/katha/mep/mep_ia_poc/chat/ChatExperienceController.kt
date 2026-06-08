package com.katha.mep.mep_ia_poc.chat

import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.models.chat.ChatError
import com.katha.mep.mep_ia_poc.models.chat.ChatEvent
import com.katha.mep.mep_ia_poc.models.chat.ChatRequest
import com.katha.mep.mep_ia_poc.models.chat.HandoffSuggested
import com.katha.mep.mep_ia_poc.models.chat.MessageCompleted
import com.katha.mep.mep_ia_poc.models.chat.MessageDelta
import com.katha.mep.mep_ia_poc.models.chat.MessageStarted
import com.katha.mep.mep_ia_poc.models.resilience.ExperienceContext
import com.katha.mep.mep_ia_poc.models.resilience.ExperienceFallback
import com.katha.mep.mep_ia_poc.observability.AiEventLogger
import com.katha.mep.mep_ia_poc.observability.MetricsCollector
import com.katha.mep.mep_ia_poc.observability.PerformanceTracker
import com.katha.mep.mep_ia_poc.resilience.ExperienceResiliencePolicy
import com.katha.mep.mep_ia_poc.usecases.chat.SendChatMessageUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

class ChatExperienceController(
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val resiliencePolicy: ExperienceResiliencePolicy,
    private val performanceTracker: PerformanceTracker,
    private val eventLogger: AiEventLogger,
    private val metricsCollector: MetricsCollector,
    private val configProvider: () -> PocConfig,
) {
    fun sendMessage(request: ChatRequest): Flow<ChatExperienceEvent> = channelFlow {
        val config = configProvider()
        val startMark = TimeSource.Monotonic.markNow()
        var timeToFirstToken: Long? = null
        var fallbackTriggered = false
        val accumulatedText = StringBuilder()
        val context = ExperienceContext(
            feature = "chat",
            userId = request.context.userId,
            providerId = config.provider.id,
            scenario = config.activeScenario.wireName,
        )

        println("[performance] requestStart")
        performanceTracker.requestStart("chat")
        metricsCollector.record("requestStart", metricAttributes(config))

        val slaJob = launch {
            delay(config.slaThresholdMillis)
            if (timeToFirstToken == null) {
                fallbackTriggered = true
                val fallback = resiliencePolicy.fallbackForTimeout(context)
                println("[ai][warning] fallback triggered reason=${fallback.reason}")
                performanceTracker.fallbackTriggered(fallback.type.name, fallback.reason)
                metricsCollector.record(
                    "fallbackTriggered",
                    metricAttributes(config) + mapOf(
                        "fallbackType" to fallback.type.name,
                        "fallbackTriggeredTime" to startMark.elapsedNow().inWholeMilliseconds,
                    ),
                )
                send(ChatExperienceEvent.FallbackTriggered(fallback))
            }
        }

        sendChatMessageUseCase(request).collect { event ->
            when (event) {
                is MessageStarted -> send(ChatExperienceEvent.MessageStarted(event.messageId))
                is MessageDelta -> {
                    if (timeToFirstToken == null) {
                        timeToFirstToken = startMark.elapsedNow().inWholeMilliseconds
                        println("[performance] firstTokenReceived latency=$timeToFirstToken")
                        performanceTracker.firstTokenReceived(event.messageId)
                        metricsCollector.record(
                            "firstTokenReceived",
                            metricAttributes(config) + mapOf("timeToFirstToken" to timeToFirstToken),
                        )
                        if (!fallbackTriggered) {
                            slaJob.cancel()
                        }
                    }
                    accumulatedText.append(event.delta)
                    send(ChatExperienceEvent.MessageDelta(event.messageId, accumulatedText.toString()))
                }
                is MessageCompleted -> {
                    slaJob.cancel()
                    val totalResponseTime = startMark.elapsedNow().inWholeMilliseconds
                    val completedText = event.text.ifBlank { accumulatedText.toString() }
                    println(
                        "[performance] responseCompleted totalResponseTime=$totalResponseTime " +
                            "timeToFirstToken=${timeToFirstToken ?: -1}",
                    )
                    performanceTracker.responseCompleted("chat")
                    metricsCollector.record(
                        "responseCompleted",
                        metricAttributes(config) + mapOf(
                            "totalResponseTime" to totalResponseTime,
                            "timeToFirstToken" to timeToFirstToken,
                        ),
                    )
                    send(ChatExperienceEvent.MessageCompleted(event.messageId, completedText))
                }
                is ChatError -> {
                    slaJob.cancel()
                    println("[ai][error] chat error code=${event.code}")
                    eventLogger.log(
                        "chatError",
                        metricAttributes(config) + mapOf("errorCode" to event.code, "message" to event.message),
                    )
                    metricsCollector.record(
                        "chatError",
                        metricAttributes(config) + mapOf("errorCode" to event.code),
                    )
                    val fallback = fallbackForError(event, context)
                    if (fallback != null) {
                        performanceTracker.fallbackTriggered(fallback.type.name, fallback.reason)
                        send(ChatExperienceEvent.FallbackTriggered(fallback))
                    }
                    send(ChatExperienceEvent.Error(event.message, event.code))
                }
                is HandoffSuggested -> {
                    val fallback = resiliencePolicy.fallbackForConnectionLost(
                        context.copy(metadata = mapOf("reason" to event.reason)),
                    )
                    send(ChatExperienceEvent.FallbackTriggered(fallback))
                }
            }
        }

        slaJob.cancel()
    }

    private fun fallbackForError(
        error: ChatError,
        context: ExperienceContext,
    ): ExperienceFallback? =
        when (error.code) {
            "offline" -> resiliencePolicy.fallbackForOffline(context)
            "connectionLost" -> resiliencePolicy.fallbackForConnectionLost(context)
            else -> null
        }

    private fun metricAttributes(config: PocConfig): Map<String, Any?> =
        mapOf(
            "provider" to config.provider.id,
            "scenario" to config.activeScenario.wireName,
        )
}

sealed interface ChatExperienceEvent {
    data class MessageStarted(val messageId: String) : ChatExperienceEvent
    data class MessageDelta(val messageId: String, val text: String) : ChatExperienceEvent
    data class MessageCompleted(val messageId: String, val text: String) : ChatExperienceEvent
    data class FallbackTriggered(val fallback: ExperienceFallback) : ChatExperienceEvent
    data class Error(val message: String, val code: String?) : ChatExperienceEvent
}
