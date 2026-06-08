package com.katha.mep.mep_ia_poc.data.remote

import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.config.SearchProfile
import com.katha.mep.mep_ia_poc.contracts.SearchService
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.search.SearchResult
import com.katha.mep.mep_ia_poc.models.search.SearchSuggestion
import com.katha.mep.mep_ia_poc.network.HttpClientProvider

class RemoteSearchService(
    private val httpClientProvider: HttpClientProvider,
    private val configProvider: () -> PocConfig,
) : SearchService {
    override suspend fun search(
        query: String,
        context: ConversationContext,
        profile: SearchProfile,
    ): SearchResult {
        val config = configProvider()
        httpClientProvider.client
        TODO("Remote search adapter pending. Mapping rules stay in api-contract.md.")
    }

    override suspend fun getSuggestions(
        context: ConversationContext,
        profile: SearchProfile,
    ): List<SearchSuggestion> = emptyList()
}
