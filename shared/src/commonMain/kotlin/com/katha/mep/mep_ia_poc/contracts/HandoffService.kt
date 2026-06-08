package com.katha.mep.mep_ia_poc.contracts

import com.katha.mep.mep_ia_poc.models.resilience.ExperienceContext
import com.katha.mep.mep_ia_poc.models.resilience.ExperienceFallback

interface HandoffService {
    suspend fun startHandoff(context: ExperienceContext): ExperienceFallback
    suspend fun openWhatsappFallback(context: ExperienceContext): ExperienceFallback
}
