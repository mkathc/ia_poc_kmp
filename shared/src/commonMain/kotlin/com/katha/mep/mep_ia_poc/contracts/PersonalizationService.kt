package com.katha.mep.mep_ia_poc.contracts

import com.katha.mep.mep_ia_poc.config.MockUserProfile
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.home.HomeExperience

interface PersonalizationService {
    suspend fun getHomeExperience(
        context: ConversationContext,
        profile: MockUserProfile,
    ): HomeExperience

    suspend fun refreshHomeExperience(
        context: ConversationContext,
        profile: MockUserProfile,
    ): HomeExperience
}
