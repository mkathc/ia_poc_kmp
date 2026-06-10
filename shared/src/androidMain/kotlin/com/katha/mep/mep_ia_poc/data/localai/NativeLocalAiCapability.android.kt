package com.katha.mep.mep_ia_poc.data.localai

import com.katha.mep.mep_ia_poc.contracts.LocalAiCapability
import com.katha.mep.mep_ia_poc.models.localai.LocalAiRequest
import com.katha.mep.mep_ia_poc.models.localai.LocalAiResult

actual class NativeLocalAiCapability actual constructor() : LocalAiCapability {
    private val engine = FakeEmergencyAssessmentEngineAndroid()

    override suspend fun execute(request: LocalAiRequest): LocalAiResult =
        engine.assess(request)
}

private class FakeEmergencyAssessmentEngineAndroid {
    fun assess(request: LocalAiRequest): LocalAiResult {
        println("[native-local-ai] generated platform=android")
        val unsafeAnswer = request.answers.firstOrNull { it.questionId == "safe_zone" }?.answer == "No"
        return LocalAiResult(
            summary = "Evaluación local Android para ${request.emergencyType.displayName}: ${request.answers.size} respuesta(s) registradas.",
            recommendedNextStep = if (unsafeAnswer) {
                "Prioriza moverte a una zona segura y contacta asistencia."
            } else {
                "Continúa con la guía offline y conserva la información del incidente."
            },
            confidence = 0.72,
        )
    }
}
