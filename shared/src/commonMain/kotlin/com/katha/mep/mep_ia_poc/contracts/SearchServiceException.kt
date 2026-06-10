package com.katha.mep.mep_ia_poc.contracts

class SearchServiceException(
    val code: String,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
