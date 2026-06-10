package com.katha.mep.mep_ia_poc.viewmodel

import com.katha.mep.mep_ia_poc.cache.LocalCacheStore
import com.katha.mep.mep_ia_poc.chat.ChatExperienceController
import com.katha.mep.mep_ia_poc.chat.ChatViewModel
import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.contracts.LocalAiCapability
import com.katha.mep.mep_ia_poc.data.local.FakeChatService
import com.katha.mep.mep_ia_poc.data.local.FakeHandoffService
import com.katha.mep.mep_ia_poc.data.local.FakePersonalizationService
import com.katha.mep.mep_ia_poc.data.local.FakeSearchService
import com.katha.mep.mep_ia_poc.data.local.LocalEmergencySupportService
import com.katha.mep.mep_ia_poc.home.HomeViewModel
import com.katha.mep.mep_ia_poc.observability.AiEventLogger
import com.katha.mep.mep_ia_poc.observability.MetricsCollector
import com.katha.mep.mep_ia_poc.observability.PerformanceTracker
import com.katha.mep.mep_ia_poc.resilience.ExperienceResiliencePolicy
import com.katha.mep.mep_ia_poc.search.SearchViewModel
import com.katha.mep.mep_ia_poc.models.localai.LocalAiRequest
import com.katha.mep.mep_ia_poc.models.localai.LocalAiResult
import com.katha.mep.mep_ia_poc.usecases.chat.SendChatMessageUseCase
import com.katha.mep.mep_ia_poc.usecases.emergency.CreateEmergencyReportUseCase
import com.katha.mep.mep_ia_poc.usecases.emergency.GetEmergencyGuideUseCase
import com.katha.mep.mep_ia_poc.usecases.emergency.GetPendingEmergencyReportsUseCase
import com.katha.mep.mep_ia_poc.usecases.emergency.SyncPendingEmergencyActionsUseCase
import com.katha.mep.mep_ia_poc.usecases.home.GetHomeExperienceUseCase
import com.katha.mep.mep_ia_poc.usecases.localai.ExecuteLocalAiCapabilityUseCase
import com.katha.mep.mep_ia_poc.usecases.search.SearchUseCase

fun previewAppViewModel(): AppViewModel =
    AppViewModel(
        initialConfig = PocConfig(),
        updatePocConfig = {},
    )

fun previewChatViewModel(appViewModel: AppViewModel): ChatViewModel {
    val logger = AiEventLogger()
    val config = PocConfig()
    val cacheStore = LocalCacheStore()
    val emergencySupportService = LocalEmergencySupportService(cacheStore)
    val controller = ChatExperienceController(
        sendChatMessageUseCase = SendChatMessageUseCase(FakeChatService { config }),
        resiliencePolicy = ExperienceResiliencePolicy(),
        performanceTracker = PerformanceTracker(logger),
        eventLogger = logger,
        metricsCollector = MetricsCollector(),
        configProvider = { config },
    )
    return ChatViewModel(
        appState = appViewModel.state,
        changeScenario = appViewModel::changeScenario,
        changeProvider = appViewModel::changeProvider,
        controller = controller,
        handoffService = FakeHandoffService(),
        getEmergencyGuideUseCase = GetEmergencyGuideUseCase(emergencySupportService),
        createEmergencyReportUseCase = CreateEmergencyReportUseCase(emergencySupportService),
        getPendingEmergencyReportsUseCase = GetPendingEmergencyReportsUseCase(emergencySupportService),
        syncPendingEmergencyActionsUseCase = SyncPendingEmergencyActionsUseCase(emergencySupportService),
        executeLocalAiCapabilityUseCase = ExecuteLocalAiCapabilityUseCase(
            localAiCapability = PreviewLocalAiCapability(),
            metricsCollector = MetricsCollector(),
        ),
        performanceTracker = PerformanceTracker(logger),
        eventLogger = logger,
        metricsCollector = MetricsCollector(),
        configProvider = { config },
    )
}

fun previewHomeViewModel(appViewModel: AppViewModel): HomeViewModel {
    val logger = AiEventLogger()
    val config = PocConfig()
    return HomeViewModel(
        appState = appViewModel.state,
        changeScenario = appViewModel::changeScenario,
        changeProvider = appViewModel::changeProvider,
        toggleRemoteGateway = appViewModel::toggleRemoteGateway,
        getHomeExperienceUseCase = GetHomeExperienceUseCase(
            personalizationServiceProvider = { FakePersonalizationService { config } },
            cacheStore = LocalCacheStore(),
            configProvider = { config },
        ),
        performanceTracker = PerformanceTracker(logger),
        eventLogger = logger,
        metricsCollector = MetricsCollector(),
        configProvider = { config },
    )
}

fun previewSearchViewModel(appViewModel: AppViewModel): SearchViewModel {
    val logger = AiEventLogger()
    val config = PocConfig()
    return SearchViewModel(
        appState = appViewModel.state,
        changeScenario = appViewModel::changeScenario,
        changeProvider = appViewModel::changeProvider,
        toggleRemoteGateway = appViewModel::toggleRemoteGateway,
        searchUseCase = SearchUseCase(
            searchServiceProvider = { FakeSearchService { config } },
            cacheStore = LocalCacheStore(),
            configProvider = { config },
        ),
        performanceTracker = PerformanceTracker(logger),
        eventLogger = logger,
        metricsCollector = MetricsCollector(),
        configProvider = { config },
    )
}

private class PreviewLocalAiCapability : LocalAiCapability {
    override suspend fun execute(request: LocalAiRequest): LocalAiResult =
        LocalAiResult(
            summary = "Preview local assessment for ${request.emergencyType.displayName}.",
            recommendedNextStep = "Continue with the offline guide.",
            confidence = 0.7,
        )
}
