package com.katha.mep.mep_ia_poc.usecases.home

import com.katha.mep.mep_ia_poc.cache.LocalCacheStore
import com.katha.mep.mep_ia_poc.config.MockUserProfile
import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.contracts.PersonalizationService
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.home.HomeExperience

class GetHomeExperienceUseCase(
    private val personalizationService: PersonalizationService,
    private val cacheStore: LocalCacheStore,
    private val configProvider: () -> PocConfig,
) {
    suspend operator fun invoke(
        context: ConversationContext,
        profile: MockUserProfile,
    ): HomeExperience {
        val config = configProvider()
        val experience = personalizationService.getHomeExperience(context, profile)
        cacheStore.save(LocalCacheStore.homeKey(config.provider.id, profile.profileName), experience)
        return experience
    }
}
