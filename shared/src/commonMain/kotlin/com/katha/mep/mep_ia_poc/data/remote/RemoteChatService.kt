package com.katha.mep.mep_ia_poc.data.remote

import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.contracts.ChatService
import com.katha.mep.mep_ia_poc.models.chat.ChatEvent
import com.katha.mep.mep_ia_poc.models.chat.ChatError
import com.katha.mep.mep_ia_poc.models.chat.ChatMessage
import com.katha.mep.mep_ia_poc.models.chat.ChatRequest
import com.katha.mep.mep_ia_poc.models.chat.HandoffSuggested
import com.katha.mep.mep_ia_poc.models.chat.MessageCompleted
import com.katha.mep.mep_ia_poc.models.chat.MessageDelta
import com.katha.mep.mep_ia_poc.models.chat.MessageStarted
import com.katha.mep.mep_ia_poc.network.SseEvent
import com.katha.mep.mep_ia_poc.network.SseHttpException
import com.katha.mep.mep_ia_poc.network.SseClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class RemoteChatService(
    private val sseClient: SseClient,
    private val configProvider: () -> PocConfig,
) : ChatService {
    override fun sendMessage(request: ChatRequest): Flow<ChatEvent> = flow {
        val config = configProvider()
        val scenario = config.activeScenario.wireName
        val provider = config.provider.id
        val baseUrl = config.remoteGatewayBaseUrl.trimEnd('/')
        val url = "$baseUrl/chat/stream?scenario=$scenario&provider=$provider"
        val body = request.toJsonBody(provider = provider, scenario = scenario)

        println("[RemoteChatService] scenario=$scenario provider=$provider baseUrl=$baseUrl")

        var started = false
        var terminal = false
        var currentMessageId: String? = null
        val accumulatedText = StringBuilder()

        try {
            sseClient.streamPost(
                url = url,
                body = body,
                headers = mapOf(
                    "Accept" to "text/event-stream",
                    "Content-Type" to "application/json",
                ),
            ).collect { sseEvent ->
                println("[SSE] event=${sseEvent.event} data=${sseEvent.data}")
                when (val chatEvent = sseEvent.toChatEvent(accumulatedText.toString())) {
                    is MessageStarted -> {
                        started = true
                        currentMessageId = chatEvent.messageId
                        emit(chatEvent)
                    }
                    is MessageDelta -> {
                        started = true
                        currentMessageId = chatEvent.messageId
                        accumulatedText.append(chatEvent.delta)
                        println("[Mapper] delta=${chatEvent.delta}")
                        emit(chatEvent)
                    }
                    is MessageCompleted -> {
                        terminal = true
                        currentMessageId = chatEvent.messageId
                        val completed = if (chatEvent.text.isBlank()) {
                            chatEvent.copy(text = accumulatedText.toString())
                        } else {
                            chatEvent
                        }
                        println("[Mapper] completed=${completed.text}")
                        emit(completed)
                    }
                    is ChatError -> {
                        terminal = true
                        currentMessageId = chatEvent.messageId
                        emit(chatEvent)
                    }
                    is HandoffSuggested -> {
                        currentMessageId = chatEvent.messageId
                        emit(chatEvent)
                    }
                }
            }

            if (started && !terminal) {
                emit(
                    ChatError(
                        messageId = currentMessageId ?: "connection-lost",
                        message = "Connection lost before completion",
                        code = "connectionLost",
                    ),
                )
            }
        } catch (exception: SseHttpException) {
            val code = if (exception.statusCode == 503) "offline" else "http_error"
            emit(ChatError(currentMessageId ?: "http-error", exception.message ?: "HTTP error", code))
        } catch (exception: Throwable) {
            emit(ChatError(currentMessageId ?: "stream-error", exception.message ?: "Stream error", "stream_error"))
        }
    }

    override suspend fun cancelMessage(messageId: String) = Unit

    override suspend fun getConversation(conversationId: String): List<ChatMessage> = emptyList()
}

private fun ChatRequest.toJsonBody(provider: String, scenario: String): String =
    buildJsonObject {
        put("conversationId", conversationId)
        put("message", message)
        put(
            "context",
            buildJsonObject {
                put("userId", context.userId)
                put("channel", context.channel)
                put("journey", context.journey)
                put("locale", context.locale)
            },
        )
        put("provider", provider)
        put("scenario", scenario)
    }.toString()

private fun SseEvent.toChatEvent(accumulatedText: String): ChatEvent {
    val payload = runCatching { Json.parseToJsonElement(data).jsonObject }.getOrElse { JsonObject(emptyMap()) }
    val messageId = payload.stringValue("messageId").ifBlank { "remote-message" }

    return when (event) {
        "message.started" -> MessageStarted(messageId)
        "message.delta" -> MessageDelta(
            messageId = messageId,
            delta = payload.firstString("delta", "text", "content"),
        )
        "message.completed" -> MessageCompleted(
            messageId = messageId,
            text = payload.firstString("text", "content", "message").ifBlank { accumulatedText },
        )
        "error" -> ChatError(
            messageId = messageId,
            message = payload.firstString("message", "error").ifBlank { "Controlled error" },
            code = payload.stringValue("code").ifBlank { null },
        )
        "handoff.suggested" -> HandoffSuggested(
            messageId = messageId,
            reason = payload.stringValue("reason").ifBlank { "Whatsapp handoff suggested" },
        )
        else -> MessageDelta(messageId, payload.firstString("delta", "text", "content"))
    }
}

private fun JsonObject.firstString(vararg names: String): String =
    names.firstNotNullOfOrNull { name -> stringValue(name).takeIf { it.isNotBlank() } }.orEmpty()

private fun JsonObject.stringValue(name: String): String =
    (this[name] as? JsonPrimitive)?.jsonPrimitive?.content.orEmpty()
