package com.katha.mep.mep_ia_poc.models.search

data class SearchResult(
    val query: String,
    val detectedIntent: String,
    val results: List<SearchItem> = emptyList(),
    val suggestedActions: List<SearchSuggestedAction> = emptyList(),
    val isFromCache: Boolean = false,
)

data class SearchItem(
    val id: String,
    val title: String,
    val description: String,
    val route: String,
    val score: Double,
)

data class SearchSuggestion(
    val id: String,
    val label: String,
)

data class SearchSuggestedAction(
    val id: String,
    val label: String,
    val type: String,
    val payload: Map<String, Any?> = emptyMap(),
)
