package com.katha.mep.mep_ia_poc.search

import com.katha.mep.mep_ia_poc.config.FakeGatewayScenario
import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.config.ProviderMock
import com.katha.mep.mep_ia_poc.config.SearchProfile
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.search.SearchItem
import com.katha.mep.mep_ia_poc.models.search.SearchSuggestedAction
import com.katha.mep.mep_ia_poc.observability.AiEventLogger
import com.katha.mep.mep_ia_poc.observability.MetricsCollector
import com.katha.mep.mep_ia_poc.observability.PerformanceTracker
import com.katha.mep.mep_ia_poc.state.PocDebugState
import com.katha.mep.mep_ia_poc.usecases.search.SearchUseCase
import com.katha.mep.mep_ia_poc.usecases.search.SearchUseCaseResult
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

class SearchViewModel(
    private val appState: StateFlow<PocDebugState>,
    private val changeScenario: (FakeGatewayScenario) -> Unit,
    private val changeProvider: (ProviderMock) -> Unit,
    private val toggleRemoteGateway: (Boolean) -> Unit,
    private val searchUseCase: SearchUseCase,
    private val performanceTracker: PerformanceTracker,
    private val eventLogger: AiEventLogger,
    private val metricsCollector: MetricsCollector,
    private val configProvider: () -> PocConfig,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var activeSearchJob: Job? = null
    private var lastSubmittedQuery: String = ""
    private val mutableState = MutableStateFlow(
        SearchState(
            activeScenario = appState.value.activeScenario,
            activeProvider = appState.value.activeProvider,
            useRemoteGateway = appState.value.useRemoteGateway,
        ),
    )
    val state: StateFlow<SearchState> = mutableState.asStateFlow()

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
    }

    fun updateQuery(query: String) {
        mutableState.update { it.copy(query = query, errorMessage = null) }
    }

    fun search() {
        val query = state.value.query.trim()
        if (query.isBlank()) return
        lastSubmittedQuery = query
        searchInternal(query)
    }

    fun clear() {
        activeSearchJob?.cancel()
        lastSubmittedQuery = ""
        mutableState.update {
            SearchState(
                activeScenario = it.activeScenario,
                activeProvider = it.activeProvider,
                activeProfile = it.activeProfile,
                useRemoteGateway = it.useRemoteGateway,
            )
        }
    }

    fun retry() {
        val query = lastSubmittedQuery.ifBlank { state.value.query.trim() }
        if (query.isBlank()) return
        mutableState.update { it.copy(query = query) }
        searchInternal(query)
    }

    fun changeScenario(scenario: FakeGatewayScenario) {
        changeScenario.invoke(scenario)
        refreshCurrentQuery()
    }

    fun changeProvider(provider: ProviderMock) {
        changeProvider.invoke(provider)
        refreshCurrentQuery()
    }

    fun changeProfile(profile: SearchProfile) {
        mutableState.update { it.copy(activeProfile = profile) }
        refreshCurrentQuery()
    }

    fun toggleRemoteGateway(useRemoteGateway: Boolean) {
        toggleRemoteGateway.invoke(useRemoteGateway)
        refreshCurrentQuery()
    }

    fun onSuggestionTapped(text: String) {
        mutableState.update { it.copy(query = text) }
        lastSubmittedQuery = text
        searchInternal(text)
    }

    fun onResultTapped(result: SearchItem) {
        println("[search] result tapped id=${result.id} route=${result.route}")
        eventLogger.log("searchResultTapped", mapOf("id" to result.id, "route" to result.route))
    }

    fun onActionTapped(action: SearchSuggestedAction) {
        println("[search] action tapped id=${action.id} type=${action.type} payload=${action.payload}")
        eventLogger.log(
            "searchActionTapped",
            mapOf("id" to action.id, "type" to action.type, "payload" to action.payload),
        )
    }

    private fun refreshCurrentQuery() {
        val query = lastSubmittedQuery.ifBlank { state.value.query.trim() }
        if (query.isNotBlank()) {
            mutableState.update { it.copy(query = query) }
            searchInternal(query)
        }
    }

    private fun searchInternal(query: String) {
        activeSearchJob?.cancel()
        activeSearchJob = scope.launch {
            val profile = state.value.activeProfile
            val config = configProvider()
            val startMark = TimeSource.Monotonic.markNow()
            var completed = false

            println("[performance] searchRequestStart")
            println("[search] request")
            eventLogger.log("searchRequestStart", metricAttributes(config, profile, query))
            metricsCollector.record("searchRequestStart", metricAttributes(config, profile, query))
            performanceTracker.requestStart("search")

            mutableState.update {
                it.copy(
                    isLoading = it.results.isEmpty(),
                    isSearching = true,
                    isFromCache = false,
                    degradedMessage = null,
                    errorMessage = null,
                    hasSearched = true,
                )
            }

            val degradedJob = launch {
                delay(config.slaThresholdMillis)
                if (!completed) {
                    println("[search] degraded experience reason=sla_timeout")
                    eventLogger.log("searchDegradedExperience", metricAttributes(config, profile, query) + mapOf("reason" to "sla_timeout"))
                    mutableState.update {
                        it.copy(degradedMessage = "La búsqueda está tardando más de lo esperado.")
                    }
                }
            }

            val result = searchUseCase(query, SearchContext, profile)
            completed = true
            degradedJob.cancel()

            val elapsed = startMark.elapsedNow().inWholeMilliseconds
            println("[performance] searchResponseCompleted totalSearchTime=$elapsed")
            performanceTracker.searchResponseTime(elapsed)
            metricsCollector.record(
                "searchResponseCompleted",
                metricAttributes(config, profile, query) + mapOf("totalSearchTime" to elapsed),
            )

            when (result) {
                is SearchUseCaseResult.Success -> {
                    println("[search] results received count=${result.result.results.size}")
                    println("[search] intent detected type=${result.result.detectedIntent}")
                    eventLogger.log(
                        "searchLoaded",
                        metricAttributes(config, profile, query) + result.result.metricData(),
                    )
                    mutableState.update {
                        it.copy(
                            results = result.result.results,
                            suggestedActions = result.result.suggestedActions,
                            detectedIntent = result.result.detectedIntent,
                            isLoading = false,
                            isSearching = false,
                            isFromCache = false,
                            degradedMessage = null,
                            errorMessage = null,
                        )
                    }
                }
                is SearchUseCaseResult.Cached -> {
                    println("[search] cache hit")
                    eventLogger.log("searchCacheHit", metricAttributes(config, profile, query) + mapOf("code" to result.code))
                    metricsCollector.record("searchCacheHit", metricAttributes(config, profile, query) + mapOf("code" to result.code))
                    mutableState.update {
                        it.copy(
                            results = result.result.results,
                            suggestedActions = result.result.suggestedActions,
                            detectedIntent = result.result.detectedIntent,
                            isLoading = false,
                            isSearching = false,
                            isFromCache = true,
                            degradedMessage = result.message,
                            errorMessage = null,
                        )
                    }
                }
                is SearchUseCaseResult.Error -> {
                    println("[search] error code=${result.code}")
                    eventLogger.log("searchError", metricAttributes(config, profile, query) + mapOf("code" to result.code))
                    metricsCollector.record("searchError", metricAttributes(config, profile, query) + mapOf("code" to result.code))
                    metricsCollector.record("searchCacheMiss", metricAttributes(config, profile, query) + mapOf("code" to result.code))
                    mutableState.update {
                        it.copy(
                            results = emptyList(),
                            suggestedActions = emptyList(),
                            detectedIntent = "unknown",
                            isLoading = false,
                            isSearching = false,
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
        profile: SearchProfile,
        query: String,
    ): Map<String, Any?> =
        mapOf(
            "provider" to config.provider.id,
            "scenario" to config.activeScenario.wireName,
            "profile" to profile.profileName,
            "query" to query,
        )

    private fun com.katha.mep.mep_ia_poc.models.search.SearchResult.metricData(): Map<String, Any?> =
        mapOf(
            "intent" to detectedIntent,
            "resultCount" to results.size,
            "suggestedActionCount" to suggestedActions.size,
        )

    private companion object {
        val SearchContext = ConversationContext(
            userId = "user-001",
            channel = "kmp",
            journey = "intelligent-search-poc",
        )
    }
}
