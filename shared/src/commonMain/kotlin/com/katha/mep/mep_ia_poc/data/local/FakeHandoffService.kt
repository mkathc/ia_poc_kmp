package com.katha.mep.mep_ia_poc.data.local

import com.katha.mep.mep_ia_poc.contracts.HandoffService
import com.katha.mep.mep_ia_poc.models.resilience.ExperienceContext
import com.katha.mep.mep_ia_poc.models.resilience.ExperienceFallback
import com.katha.mep.mep_ia_poc.models.resilience.ExperienceFallbackType

class FakeHandoffService : HandoffService {
    override suspend fun startHandoff(context: ExperienceContext): ExperienceFallback =
        openWhatsappFallback(context)

    override suspend fun openWhatsappFallback(context: ExperienceContext): ExperienceFallback =
        ExperienceFallback(
            type = ExperienceFallbackType.WhatsappHandoff,
            reason = "Whatsapp handoff suggested",
            actionLabel = "Abrir Whatsapp",
            metadata = context.metadata,
        )
}
