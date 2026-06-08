package com.katha.mep.mep_ia_poc.data.local

import com.katha.mep.mep_ia_poc.config.MockUserProfile
import com.katha.mep.mep_ia_poc.contracts.PersonalizationService
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.home.ActionSuggestion
import com.katha.mep.mep_ia_poc.models.home.HomeCard
import com.katha.mep.mep_ia_poc.models.home.HomeExperience

class FakePersonalizationService : PersonalizationService {
    override suspend fun getHomeExperience(
        context: ConversationContext,
        profile: MockUserProfile,
    ): HomeExperience = buildExperience(profile)

    override suspend fun refreshHomeExperience(
        context: ConversationContext,
        profile: MockUserProfile,
    ): HomeExperience = buildExperience(profile)

    private fun buildExperience(profile: MockUserProfile): HomeExperience =
        HomeExperience(
            userSegment = profile.profileName,
            cards = listOf(
                HomeCard(
                    id = "base-card",
                    type = "placeholder",
                    title = "Experiencia lista",
                    description = "Base preparada para personalizacion IA-first.",
                    priority = 1,
                    action = ActionSuggestion("base-action", "Ver detalle", "/home/detail"),
                ),
            ),
            nextBestActions = listOf(ActionSuggestion("continue", "Continuar", "/next")),
        )
}
