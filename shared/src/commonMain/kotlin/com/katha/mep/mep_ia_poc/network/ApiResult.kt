package com.katha.mep.mep_ia_poc.network

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(
        val code: String,
        val message: String,
        val throwable: Throwable? = null,
    ) : ApiResult<Nothing>
}
