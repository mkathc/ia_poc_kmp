package com.katha.mep.mep_ia_poc.state

import com.katha.mep.mep_ia_poc.config.FakeGatewayScenario
import com.katha.mep.mep_ia_poc.config.ProviderMock

data class PocDebugState(
    val activeScenario: FakeGatewayScenario,
    val activeProvider: ProviderMock,
    val useRemoteGateway: Boolean,
    val remoteGatewayBaseUrl: String,
    val enableLocalAiReadiness: Boolean,
)
