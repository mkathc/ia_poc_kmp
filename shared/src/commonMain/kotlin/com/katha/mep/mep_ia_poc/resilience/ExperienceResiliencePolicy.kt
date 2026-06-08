package com.katha.mep.mep_ia_poc.resilience

import com.katha.mep.mep_ia_poc.models.resilience.ExperienceContext
import com.katha.mep.mep_ia_poc.models.resilience.ExperienceFallback
import com.katha.mep.mep_ia_poc.models.resilience.ExperienceFallbackType

class ExperienceResiliencePolicy {
    fun fallbackForTimeout(context: ExperienceContext): ExperienceFallback =
        ExperienceFallback(
            type = ExperienceFallbackType.SlaTimeout,
            reason = "SLA threshold exceeded",
            actionLabel = "Continuar en modo asistido",
            metadata = context.metadata,
        )

    fun fallbackForOffline(context: ExperienceContext): ExperienceFallback =
        ExperienceFallback(
            type = ExperienceFallbackType.Offline,
            reason = "Offline experience available",
            actionLabel = "Usar experiencia local",
            metadata = context.metadata,
        )

    fun fallbackForConnectionLost(context: ExperienceContext): ExperienceFallback =
        ExperienceFallback(
            type = ExperienceFallbackType.ConnectionLost,
            reason = "Connection lost during experience",
            actionLabel = "Continuar localmente",
            metadata = context.metadata,
        )

    fun fallbackForCachedExperience(context: ExperienceContext): ExperienceFallback =
        ExperienceFallback(
            type = ExperienceFallbackType.CachedExperience,
            reason = "Using cached degraded experience",
            metadata = context.metadata,
        )
}
