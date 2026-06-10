package com.katha.mep.mep_ia_poc.data.localai

import com.katha.mep.mep_ia_poc.contracts.LocalAiCapability
import com.katha.mep.mep_ia_poc.models.localai.LocalAiRequest
import com.katha.mep.mep_ia_poc.models.localai.LocalAiResult

actual class NativeLocalAiCapability actual constructor() : LocalAiCapability {
    private val engine = FakeEmergencyAssessmentEngineIos()

    override suspend fun execute(request: LocalAiRequest): LocalAiResult =
        engine.assess(request)
}

private class FakeEmergencyAssessmentEngineIos {
    fun assess(request: LocalAiRequest): LocalAiResult {
        println("[native-local-ai] generated platform=ios")
        val injuredAnswer = request.answers.firstOrNull { it.questionId == "injured_people" }?.answer
        return LocalAiResult(
            summary = "Evaluación local iOS para ${request.emergencyType.displayName}: ${request.answers.size} respuesta(s) registradas.",
            recommendedNextStep = if (injuredAnswer == "Sí" || injuredAnswer == "No estoy seguro") {
                "Contacta la central de emergencias y sigue las instrucciones del operador."
            } else {
                "Continúa documentando el incidente cuando sea seguro."
            },
            confidence = 0.74,
        )
    }
}
