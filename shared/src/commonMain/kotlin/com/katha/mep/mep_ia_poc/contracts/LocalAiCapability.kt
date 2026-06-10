package com.katha.mep.mep_ia_poc.contracts

import com.katha.mep.mep_ia_poc.models.localai.LocalAiRequest
import com.katha.mep.mep_ia_poc.models.localai.LocalAiResult

interface LocalAiCapability {
    suspend fun execute(request: LocalAiRequest): LocalAiResult
}
