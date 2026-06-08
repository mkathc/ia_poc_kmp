package com.katha.mep.mep_ia_poc.data.local

import com.katha.mep.mep_ia_poc.config.FakeGatewayScenario
import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.config.SearchProfile
import com.katha.mep.mep_ia_poc.contracts.SearchService
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.search.SearchItem
import com.katha.mep.mep_ia_poc.models.search.SearchResult
import com.katha.mep.mep_ia_poc.models.search.SearchSuggestedAction
import com.katha.mep.mep_ia_poc.models.search.SearchSuggestion

class FakeSearchService(
    private val configProvider: () -> PocConfig = { PocConfig() },
) : SearchService {
    override suspend fun search(
        query: String,
        context: ConversationContext,
        profile: SearchProfile,
    ): SearchResult {
        val config = configProvider()
        if (config.activeScenario == FakeGatewayScenario.noResults) {
            return SearchResult(query, detectedIntent = "unknown")
        }

        return SearchResult(
            query = query,
            detectedIntent = "${profile.journey}_intent",
            results = listOf(
                SearchItem("result-1", "Resultado base", "Placeholder de busqueda.", "/search/result", 0.9),
            ),
            suggestedActions = listOf(
                SearchSuggestedAction("action-1", "Abrir resultado", "assisted_navigation", mapOf("route" to "/search/result")),
            ),
        )
    }

    override suspend fun getSuggestions(
        context: ConversationContext,
        profile: SearchProfile,
    ): List<SearchSuggestion> = emptyList()
}
