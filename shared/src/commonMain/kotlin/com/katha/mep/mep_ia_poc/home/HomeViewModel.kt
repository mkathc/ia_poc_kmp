package com.katha.mep.mep_ia_poc.home

import com.katha.mep.mep_ia_poc.config.FakeGatewayScenario
import com.katha.mep.mep_ia_poc.config.MockUserProfile
import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.config.ProviderMock
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.home.ActionSuggestion
import com.katha.mep.mep_ia_poc.observability.AiEventLogger
import com.katha.mep.mep_ia_poc.observability.MetricsCollector
import com.katha.mep.mep_ia_poc.observability.PerformanceTracker
import com.katha.mep.mep_ia_poc.state.PocDebugState
import com.katha.mep.mep_ia_poc.usecases.home.GetHomeExperienceUseCase
import com.katha.mep.mep_ia_poc.usecases.home.HomeExperienceResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

class HomeViewModel(
    private val appState: StateFlow<PocDebugState>,
    private val changeScenario: (FakeGatewayScenario) -> Unit,
    private val changeProvider: (ProviderMock) -> Unit,
    private val toggleRemoteGateway: (Boolean) -> Unit,
    private val getHomeExperienceUseCase: GetHomeExperienceUseCase,
    private val performanceTracker: PerformanceTracker,
    private val eventLogger: AiEventLogger,
    private val metricsCollector: MetricsCollector,
    private val configProvider: () -> PocConfig,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var activeLoadJob: Job? = null
    private val mutableState = MutableStateFlow(
        HomeState(
            activeScenario = appState.value.activeScenario,
            activeProvider = appState.value.activeProvider,
            useRemoteGateway = appState.value.useRemoteGateway,
        ),
    )
    val state: StateFlow<HomeState> = mutableState.asStateFlow()

    init {
        scope.launch {
            appState.collect { debugState ->
                mutableState.update {
                    it.copy(
                        activeScenario = debugState.activeScenario,
                        activeProvider = debugState.activeProvider,
                        useRemoteGateway = debugState.useRemoteGateway,
                    )
                }
            }
        }
        load()
    }

    fun load() {
        loadInternal(isRefresh = false)
    }

    fun refresh() {
        loadInternal(isRefresh = true)
    }

    fun changeScenario(scenario: FakeGatewayScenario) {
        changeScenario.invoke(scenario)
        loadInternal(isRefresh = true)
    }

    fun changeProvider(provider: ProviderMock) {
        changeProvider.invoke(provider)
        loadInternal(isRefresh = true)
    }

    fun changeProfile(profile: MockUserProfile) {
        mutableState.update { it.copy(activeProfile = profile) }
        loadInternal(isRefresh = true)
    }

    fun toggleRemoteGateway(useRemoteGateway: Boolean) {
        toggleRemoteGateway.invoke(useRemoteGateway)
        loadInternal(isRefresh = true)
    }

    fun onActionTapped(action: ActionSuggestion) {
        println("[home] action tapped id=${action.id} route=${action.route}")
        eventLogger.log("homeActionTapped", mapOf("id" to action.id, "route" to action.route))
    }

    private fun loadInternal(isRefresh: Boolean) {
        activeLoadJob?.cancel()
        activeLoadJob = scope.launch {
            val profile = state.value.activeProfile
            val config = configProvider()
            val startMark = TimeSource.Monotonic.markNow()
            var completed = false

            println("[performance] homeRequestStart")
            eventLogger.log("homeRequestStart", metricAttributes(config, profile))
            metricsCollector.record("homeRequestStart", metricAttributes(config, profile))
            performanceTracker.requestStart("home")

            mutableState.update {
                it.copy(
                    isLoading = !isRefresh && it.experience == null,
                    isRefreshing = isRefresh,
                    degradedMessage = null,
                    errorMessage = null,
                )
            }

            val degradedJob = launch {
                delay(config.slaThresholdMillis)
                if (!completed) {
                    println("[home] degraded experience reason=sla_timeout")
                    eventLogger.log("homeDegradedExperience", metricAttributes(config, profile) + mapOf("reason" to "sla_timeout"))
                    mutableState.update {
                        it.copy(degradedMessage = "Estamos tardando más de lo esperado en personalizar Home.")
                    }
                }
            }

            val result = getHomeExperienceUseCase(HomeContext, profile)
            completed = true
            degradedJob.cancel()

            val elapsed = startMark.elapsedNow().inWholeMilliseconds
            println("[performance] homeResponseCompleted totalHomeRefreshTime=$elapsed")
            performanceTracker.homeRefreshTime(elapsed)
            metricsCollector.record(
                "homeResponseCompleted",
                metricAttributes(config, profile) + mapOf("totalHomeRefreshTime" to elapsed),
            )

            when (result) {
                is HomeExperienceResult.Success -> {
                    eventLogger.log("homeLoaded", metricAttributes(config, profile))
                    mutableState.update {
                        it.copy(
                            experience = result.experience,
                            isLoading = false,
                            isRefreshing = false,
                            isFromCache = false,
                            degradedMessage = null,
                            errorMessage = null,
                        )
                    }
                }
                is HomeExperienceResult.Cached -> {
                    println("[home] cache hit")
                    eventLogger.log("homeCacheHit", metricAttributes(config, profile) + mapOf("code" to result.code))
                    metricsCollector.record("cacheHit", metricAttributes(config, profile) + mapOf("code" to result.code))
                    mutableState.update {
                        it.copy(
                            experience = result.experience,
                            isLoading = false,
                            isRefreshing = false,
                            isFromCache = true,
                            degradedMessage = result.message,
                            errorMessage = null,
                        )
                    }
                }
                is HomeExperienceResult.Error -> {
                    println("[home] error code=${result.code}")
                    eventLogger.log("homeError", metricAttributes(config, profile) + mapOf("code" to result.code))
                    metricsCollector.record("homeError", metricAttributes(config, profile) + mapOf("code" to result.code))
                    metricsCollector.record("cacheMiss", metricAttributes(config, profile) + mapOf("code" to result.code))
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isFromCache = false,
                            degradedMessage = null,
                            errorMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    private fun metricAttributes(
        config: PocConfig,
        profile: MockUserProfile,
    ): Map<String, Any?> =
        mapOf(
            "provider" to config.provider.id,
            "scenario" to config.activeScenario.wireName,
            "profile" to profile.profileName,
        )

    private companion object {
        val HomeContext = ConversationContext(
            userId = "user-001",
            channel = "kmp",
            journey = "home-personalization-poc",
        )
    }
}
