package com.katha.mep.mep_ia_poc.chat

import com.katha.mep.mep_ia_poc.config.FakeGatewayScenario
import com.katha.mep.mep_ia_poc.config.ProviderMock
import com.katha.mep.mep_ia_poc.models.chat.ChatMessage
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyAnswer
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyConversationStep
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyGuide
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyReport
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyType
import com.katha.mep.mep_ia_poc.models.localai.LocalAiResult
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
    val selectedEmergencyType: EmergencyType? = null,
    val activeEmergencyGuide: EmergencyGuide? = null,
    val emergencyAnswers: List<EmergencyAnswer> = emptyList(),
    val currentEmergencyStep: EmergencyConversationStep? = null,
    val emergencyConversationCompleted: Boolean = false,
    val pendingEmergencyReports: List<EmergencyReport> = emptyList(),
    val isEmergencySyncing: Boolean = false,
    val emergencyMessage: String? = null,
    val localAiResult: LocalAiResult? = null,
    val lastUserMessage: String? = null,
)
