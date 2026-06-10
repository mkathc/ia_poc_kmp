package com.katha.mep.mep_ia_poc.usecases.search

import com.katha.mep.mep_ia_poc.config.SearchProfile
import com.katha.mep.mep_ia_poc.contracts.SearchService
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.search.SearchSuggestion

class GetSearchSuggestionsUseCase(
    private val searchServiceProvider: () -> SearchService,
) {
    suspend operator fun invoke(
        context: ConversationContext,
        profile: SearchProfile,
    ): List<SearchSuggestion> = searchServiceProvider().getSuggestions(context, profile)
}
