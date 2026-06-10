package com.katha.mep.mep_ia_poc.data.sdk

import com.katha.mep.mep_ia_poc.config.FakeGatewayScenario
import com.katha.mep.mep_ia_poc.contracts.ChatProviderAdapter
import com.katha.mep.mep_ia_poc.models.chat.ChatError
import com.katha.mep.mep_ia_poc.models.chat.ChatEvent
import com.katha.mep.mep_ia_poc.models.chat.ChatRequest
import com.katha.mep.mep_ia_poc.models.chat.MessageCompleted
import com.katha.mep.mep_ia_poc.models.chat.MessageDelta
import com.katha.mep.mep_ia_poc.models.chat.MessageStarted
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

actual class NativeSdkAdapter actual constructor() : ChatProviderAdapter {
    private val sdk = FakeNativeChatSdkAndroid()

    override fun sendMessage(
        request: ChatRequest,
        scenario: FakeGatewayScenario,
    ): Flow<ChatEvent> = channelFlow {
        launch {
            sdk.sendMessage(
                text = request.message,
                scenario = scenario,
                onStarted = { messageId ->
                    println("[native-sdk] started platform=android")
                    trySend(MessageStarted(messageId))
                },
                onToken = { messageId, token ->
                    println("[native-sdk] token platform=android token=$token")
                    trySend(MessageDelta(messageId, token))
                },
                onCompleted = { messageId, text ->
                    println("[native-sdk] completed platform=android")
                    trySend(MessageCompleted(messageId, text))
                    close()
                },
                onError = { messageId, message, code ->
                    println("[native-sdk] error platform=android code=$code")
                    trySend(ChatError(messageId, message, code))
                    close()
                },
            )
        }
    }
}

private class FakeNativeChatSdkAndroid {
    suspend fun sendMessage(
        text: String,
        scenario: FakeGatewayScenario,
        onStarted: (String) -> Unit,
        onToken: (String, String) -> Unit,
        onCompleted: (String, String) -> Unit,
        onError: (String, String, String) -> Unit,
    ) {
        val messageId = "android-sdk-${text.hashCode()}"
        onStarted(messageId)
        when (scenario) {
            FakeGatewayScenario.error -> {
                delay(250L)
                onError(messageId, "Native SDK controlled error", "sdk_error")
            }
            FakeGatewayScenario.timeout -> {
                delay(3_500L)
                val tokens = listOf("Respuesta ", "tardía ", "desde SDK Android.")
                tokens.forEach {
                    onToken(messageId, it)
                    delay(180L)
                }
                onCompleted(messageId, tokens.joinToString(separator = ""))
            }
            else -> {
                val tokens = listOf("Respuesta ", "desde ", "SDK Android.")
                tokens.forEach {
                    delay(180L)
                    onToken(messageId, it)
                }
                onCompleted(messageId, tokens.joinToString(separator = ""))
            }
        }
    }
}
