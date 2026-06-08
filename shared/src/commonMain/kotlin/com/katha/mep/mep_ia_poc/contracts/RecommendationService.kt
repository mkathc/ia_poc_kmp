package com.katha.mep.mep_ia_poc.contracts

import com.katha.mep.mep_ia_poc.config.MockUserProfile
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.home.ActionSuggestion

interface RecommendationService {
    suspend fun getRecommendations(
        context: ConversationContext,
        profile: MockUserProfile,
    ): List<ActionSuggestion>
}
