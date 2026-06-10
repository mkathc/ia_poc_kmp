package com.katha.mep.mep_ia_poc.usecases.search

import com.katha.mep.mep_ia_poc.models.search.SearchResult

sealed interface SearchUseCaseResult {
    data class Success(val result: SearchResult) : SearchUseCaseResult
    data class Cached(
        val result: SearchResult,
        val message: String,
        val code: String,
    ) : SearchUseCaseResult

    data class Error(
        val message: String,
        val code: String,
    ) : SearchUseCaseResult
}
