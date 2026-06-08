package com.katha.mep.mep_ia_poc.contracts

import com.katha.mep.mep_ia_poc.config.SearchProfile
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.search.SearchResult
import com.katha.mep.mep_ia_poc.models.search.SearchSuggestion

interface SearchService {
    suspend fun search(
        query: String,
        context: ConversationContext,
        profile: SearchProfile,
    ): SearchResult

    suspend fun getSuggestions(
        context: ConversationContext,
        profile: SearchProfile,
    ): List<SearchSuggestion>
}
