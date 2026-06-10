package com.katha.mep.mep_ia_poc.home

import com.katha.mep.mep_ia_poc.config.FakeGatewayScenario
import com.katha.mep.mep_ia_poc.config.MockUserProfile
import com.katha.mep.mep_ia_poc.config.ProviderMock
import com.katha.mep.mep_ia_poc.models.home.HomeExperience

data class HomeState(
    val experience: HomeExperience? = null,
    val activeScenario: FakeGatewayScenario = FakeGatewayScenario.happyPath,
    val activeProvider: ProviderMock = ProviderMock.providerA,
    val activeProfile: MockUserProfile = MockUserProfile.activeClaim,
    val useRemoteGateway: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isFromCache: Boolean = false,
    val degradedMessage: String? = null,
    val errorMessage: String? = null,
)
