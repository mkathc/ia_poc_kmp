package com.katha.mep.mep_ia_poc.chat

import com.katha.mep.mep_ia_poc.config.FakeGatewayScenario
import com.katha.mep.mep_ia_poc.config.ProviderMock
import com.katha.mep.mep_ia_poc.models.chat.ChatMessage
import com.katha.mep.mep_ia_poc.models.resilience.ExperienceFallback

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val activeScenario: FakeGatewayScenario = FakeGatewayScenario.happyPath,
    val activeProvider: ProviderMock = ProviderMock.providerA,
    val isStreaming: Boolean = false,
    val activeFallback: ExperienceFallback? = null,
    val hasSlaViolation: Boolean = false,
    val errorMessage: String? = null,
    val showEmergencyOptions: Boolean = false,
    val lastUserMessage: String? = null,
)
