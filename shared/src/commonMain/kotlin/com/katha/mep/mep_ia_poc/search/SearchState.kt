package com.katha.mep.mep_ia_poc.search

import com.katha.mep.mep_ia_poc.config.FakeGatewayScenario
import com.katha.mep.mep_ia_poc.config.ProviderMock
import com.katha.mep.mep_ia_poc.config.SearchProfile
import com.katha.mep.mep_ia_poc.models.search.SearchItem
import com.katha.mep.mep_ia_poc.models.search.SearchSuggestedAction

data class SearchState(
    val query: String = "",
    val results: List<SearchItem> = emptyList(),
    val suggestedActions: List<SearchSuggestedAction> = emptyList(),
    val detectedIntent: String = "unknown",
    val activeScenario: FakeGatewayScenario = FakeGatewayScenario.happyPath,
    val activeProvider: ProviderMock = ProviderMock.providerA,
    val activeProfile: SearchProfile = SearchProfile.claims,
    val useRemoteGateway: Boolean = false,
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val isFromCache: Boolean = false,
    val degradedMessage: String? = null,
    val errorMessage: String? = null,
    val hasSearched: Boolean = false,
)
