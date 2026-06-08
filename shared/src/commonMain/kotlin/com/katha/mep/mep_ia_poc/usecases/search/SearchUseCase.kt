package com.katha.mep.mep_ia_poc.usecases.search

import com.katha.mep.mep_ia_poc.cache.LocalCacheStore
import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.config.SearchProfile
import com.katha.mep.mep_ia_poc.contracts.SearchService
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.search.SearchResult

class SearchUseCase(
    private val searchService: SearchService,
    private val cacheStore: LocalCacheStore,
    private val configProvider: () -> PocConfig,
) {
    suspend operator fun invoke(
        query: String,
        context: ConversationContext,
        profile: SearchProfile,
    ): SearchResult {
        val config = configProvider()
        val normalizedQuery = query.trim().lowercase()
        val result = searchService.search(query, context, profile)
        cacheStore.save(LocalCacheStore.searchKey(config.provider.id, profile.profileName, normalizedQuery), result)
        return result
    }
}
