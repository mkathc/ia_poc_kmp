package com.katha.mep.mep_ia_poc.config

data class PocConfig(
    val activeScenario: FakeGatewayScenario = FakeGatewayScenario.happyPath,
    val provider: ProviderMock = ProviderMock.providerA,
    val useRemoteGateway: Boolean = false,
    val remoteGatewayBaseUrl: String = "http://10.0.2.2:3000",
    val slaThresholdMillis: Long = 3_000L,
    val enableLocalAiReadiness: Boolean = false,
)

enum class FakeGatewayScenario(val wireName: String) {
    happyPath("happyPath"),
    delayed("delayed"),
    timeout("timeout"),
    offline("offline"),
    error("error"),
    connectionLost("connectionLost"),
    longStream("longStream"),
    noResults("noResults"),
}

enum class ProviderMock(val id: String) {
    providerA("providerA"),
    providerB("providerB"),
    providerSdk("providerSdk"),
}

enum class MockUserProfile(val profileName: String) {
    activeClaim("activeClaim"),
    pendingPayment("pendingPayment"),
    newUser("newUser"),
    benefitsFocused("benefitsFocused"),
}

enum class SearchProfile(val profileName: String, val journey: String) {
    insurance("insurance", "insurance"),
    payments("payments", "payments"),
    claims("claims", "claims"),
    benefits("benefits", "benefits"),
}
