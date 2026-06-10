package com.katha.mep.mep_ia_poc.models.localai

import com.katha.mep.mep_ia_poc.models.emergency.EmergencyAnswer
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyType

enum class LocalAiCapabilityType {
    emergencyAssessment,
}

data class LocalAiRequest(
    val capabilityType: LocalAiCapabilityType,
    val emergencyType: EmergencyType,
    val answers: List<EmergencyAnswer>,
)

data class LocalAiResult(
    val summary: String,
    val recommendedNextStep: String,
    val confidence: Double,
)
