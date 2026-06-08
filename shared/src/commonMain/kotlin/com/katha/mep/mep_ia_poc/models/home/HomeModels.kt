package com.katha.mep.mep_ia_poc.models.home

data class HomeExperience(
    val userSegment: String,
    val cards: List<HomeCard> = emptyList(),
    val alerts: List<HomeAlert> = emptyList(),
    val nextBestActions: List<ActionSuggestion> = emptyList(),
    val isFromCache: Boolean = false,
    val generatedAt: String? = null,
)

data class HomeCard(
    val id: String,
    val type: String,
    val title: String,
    val description: String,
    val priority: Int,
    val action: ActionSuggestion? = null,
)

data class HomeAlert(
    val id: String,
    val title: String,
    val message: String,
    val severity: String = "info",
)

data class ActionSuggestion(
    val id: String,
    val label: String,
    val route: String,
    val type: String? = null,
    val payload: Map<String, Any?> = emptyMap(),
)
