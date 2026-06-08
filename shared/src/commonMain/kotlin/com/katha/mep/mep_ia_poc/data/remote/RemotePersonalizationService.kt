package com.katha.mep.mep_ia_poc.data.remote

import com.katha.mep.mep_ia_poc.config.MockUserProfile
import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.contracts.PersonalizationService
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.home.HomeExperience
import com.katha.mep.mep_ia_poc.network.HttpClientProvider

class RemotePersonalizationService(
    private val httpClientProvider: HttpClientProvider,
    private val configProvider: () -> PocConfig,
) : PersonalizationService {
    override suspend fun getHomeExperience(
        context: ConversationContext,
        profile: MockUserProfile,
    ): HomeExperience {
        val config = configProvider()
        httpClientProvider.client
        TODO("Remote home adapter pending. Keep request body profile-free per api-contract.md.")
    }

    override suspend fun refreshHomeExperience(
        context: ConversationContext,
        profile: MockUserProfile,
    ): HomeExperience = getHomeExperience(context, profile)
}
