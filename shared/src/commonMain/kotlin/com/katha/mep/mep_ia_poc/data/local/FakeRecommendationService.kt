package com.katha.mep.mep_ia_poc.data.local

import com.katha.mep.mep_ia_poc.config.MockUserProfile
import com.katha.mep.mep_ia_poc.contracts.RecommendationService
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.home.ActionSuggestion

class FakeRecommendationService : RecommendationService {
    override suspend fun getRecommendations(
        context: ConversationContext,
        profile: MockUserProfile,
    ): List<ActionSuggestion> = listOf(
        ActionSuggestion("recommendation-${profile.profileName}", "Accion sugerida", "/recommended"),
    )
}
