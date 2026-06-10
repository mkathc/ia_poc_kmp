package com.katha.mep.mep_ia_poc.contracts

class PersonalizationServiceException(
    val code: String,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
