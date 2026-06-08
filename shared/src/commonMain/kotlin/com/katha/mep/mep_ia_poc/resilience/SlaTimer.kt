package com.katha.mep.mep_ia_poc.resilience

import com.katha.mep.mep_ia_poc.config.PocConfig

class SlaTimer(
    private val configProvider: () -> PocConfig,
) {
    fun isExceeded(elapsedMillis: Long): Boolean =
        elapsedMillis >= configProvider().slaThresholdMillis
}
