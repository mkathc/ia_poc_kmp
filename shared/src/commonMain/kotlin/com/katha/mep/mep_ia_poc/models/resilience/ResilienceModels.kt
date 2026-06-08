package com.katha.mep.mep_ia_poc.models.resilience

data class ExperienceFallback(
    val type: ExperienceFallbackType,
    val reason: String,
    val actionLabel: String? = null,
    val metadata: Map<String, Any?> = emptyMap(),
)

enum class ExperienceFallbackType {
    SlaTimeout,
    Offline,
    ConnectionLost,
    CachedExperience,
    WhatsappHandoff,
    ControlledError,
}

data class ExperienceContext(
    val feature: String,
    val userId: String,
    val providerId: String,
    val scenario: String,
    val metadata: Map<String, Any?> = emptyMap(),
)

sealed interface ExperienceEvent {
    data class Started(val context: ExperienceContext) : ExperienceEvent
    data class FallbackTriggered(val fallback: ExperienceFallback) : ExperienceEvent
    data class Completed(val context: ExperienceContext) : ExperienceEvent
    data class Failed(val context: ExperienceContext, val code: String?) : ExperienceEvent
}

sealed interface ExperienceState {
    data object Idle : ExperienceState
    data class Loading(val context: ExperienceContext) : ExperienceState
    data class Degraded(val fallback: ExperienceFallback) : ExperienceState
    data class Ready(val context: ExperienceContext) : ExperienceState
    data class Error(val message: String, val code: String? = null) : ExperienceState
}
