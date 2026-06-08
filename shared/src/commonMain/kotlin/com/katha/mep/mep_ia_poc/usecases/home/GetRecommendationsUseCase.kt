package com.katha.mep.mep_ia_poc.usecases.home

import com.katha.mep.mep_ia_poc.config.MockUserProfile
import com.katha.mep.mep_ia_poc.contracts.RecommendationService
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.home.ActionSuggestion

class GetRecommendationsUseCase(
    private val recommendationService: RecommendationService,
) {
    suspend operator fun invoke(
        context: ConversationContext,
        profile: MockUserProfile,
    ): List<ActionSuggestion> = recommendationService.getRecommendations(context, profile)
}
