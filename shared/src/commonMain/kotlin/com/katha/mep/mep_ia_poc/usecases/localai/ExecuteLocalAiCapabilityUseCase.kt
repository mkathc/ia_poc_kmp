package com.katha.mep.mep_ia_poc.usecases.localai

import com.katha.mep.mep_ia_poc.contracts.LocalAiCapability
import com.katha.mep.mep_ia_poc.models.localai.LocalAiRequest
import com.katha.mep.mep_ia_poc.models.localai.LocalAiResult
import com.katha.mep.mep_ia_poc.observability.MetricsCollector
import kotlin.time.TimeSource

class ExecuteLocalAiCapabilityUseCase(
    private val localAiCapability: LocalAiCapability,
    private val metricsCollector: MetricsCollector,
) {
    suspend operator fun invoke(request: LocalAiRequest): LocalAiResult {
        val mark = TimeSource.Monotonic.markNow()
        println("[local-ai] started")
        val result = localAiCapability.execute(request)
        println("[local-ai] mapped")
        val elapsed = mark.elapsedNow().inWholeMilliseconds
        println("[local-ai] completed")
        metricsCollector.record("localAiExecutionTime", mapOf("elapsedMillis" to elapsed))
        return result
    }
}
