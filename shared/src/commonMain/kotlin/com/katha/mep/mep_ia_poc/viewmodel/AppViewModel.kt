package com.katha.mep.mep_ia_poc.viewmodel

import com.katha.mep.mep_ia_poc.config.FakeGatewayScenario
import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.config.ProviderMock
import com.katha.mep.mep_ia_poc.state.PocDebugState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppViewModel(
    initialConfig: PocConfig,
    private val updatePocConfig: (PocConfig) -> Unit,
) {
    private var currentConfig = initialConfig
    private val mutableState = MutableStateFlow(initialConfig.toDebugState())
    val state: StateFlow<PocDebugState> = mutableState.asStateFlow()

    fun changeScenario(scenario: FakeGatewayScenario) {
        updateConfig { copy(activeScenario = scenario) }
    }

    fun changeProvider(provider: ProviderMock) {
        updateConfig { copy(provider = provider) }
    }

    fun toggleRemoteGateway(useRemoteGateway: Boolean) {
        updateConfig { copy(useRemoteGateway = useRemoteGateway) }
    }

    fun toggleLocalAiReadiness(enableLocalAiReadiness: Boolean) {
        updateConfig { copy(enableLocalAiReadiness = enableLocalAiReadiness) }
    }

    private fun updateConfig(transform: PocConfig.() -> PocConfig) {
        val nextConfig = currentConfig.transform()
        currentConfig = nextConfig
        updatePocConfig(nextConfig)
        mutableState.update { nextConfig.toDebugState() }
    }
}

private fun PocConfig.toDebugState(): PocDebugState =
    PocDebugState(
        activeScenario = activeScenario,
        activeProvider = provider,
        useRemoteGateway = useRemoteGateway,
        remoteGatewayBaseUrl = remoteGatewayBaseUrl,
        enableLocalAiReadiness = enableLocalAiReadiness,
    )
