package com.katha.mep.mep_ia_poc.di

import com.katha.mep.mep_ia_poc.cache.LocalCacheStore
import com.katha.mep.mep_ia_poc.chat.ChatExperienceController
import com.katha.mep.mep_ia_poc.chat.ChatViewModel
import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.contracts.ChatService
import com.katha.mep.mep_ia_poc.contracts.EmergencySupportService
import com.katha.mep.mep_ia_poc.contracts.PersonalizationService
import com.katha.mep.mep_ia_poc.contracts.RecommendationService
import com.katha.mep.mep_ia_poc.contracts.SearchService
import com.katha.mep.mep_ia_poc.data.ChatServiceRouter
import com.katha.mep.mep_ia_poc.data.local.FakeChatService
import com.katha.mep.mep_ia_poc.data.local.FakeHandoffService
import com.katha.mep.mep_ia_poc.data.local.FakePersonalizationService
import com.katha.mep.mep_ia_poc.data.local.FakeRecommendationService
import com.katha.mep.mep_ia_poc.data.local.FakeSearchService
import com.katha.mep.mep_ia_poc.data.local.LocalEmergencySupportService
import com.katha.mep.mep_ia_poc.data.remote.RemoteChatService
import com.katha.mep.mep_ia_poc.data.remote.RemotePersonalizationService
import com.katha.mep.mep_ia_poc.data.remote.RemoteSearchService
import com.katha.mep.mep_ia_poc.network.HttpClientProvider
import com.katha.mep.mep_ia_poc.network.SseClient
import com.katha.mep.mep_ia_poc.observability.AiEventLogger
import com.katha.mep.mep_ia_poc.observability.MetricsCollector
import com.katha.mep.mep_ia_poc.observability.PerformanceTracker
import com.katha.mep.mep_ia_poc.resilience.ExperienceResiliencePolicy
import com.katha.mep.mep_ia_poc.resilience.NetworkMonitor
import com.katha.mep.mep_ia_poc.resilience.SlaTimer
import com.katha.mep.mep_ia_poc.usecases.chat.GetConversationUseCase
import com.katha.mep.mep_ia_poc.usecases.chat.SendChatMessageUseCase
import com.katha.mep.mep_ia_poc.usecases.emergency.CreateEmergencyReportUseCase
import com.katha.mep.mep_ia_poc.usecases.emergency.GetEmergencyGuideUseCase
import com.katha.mep.mep_ia_poc.usecases.emergency.SyncPendingEmergencyActionsUseCase
import com.katha.mep.mep_ia_poc.usecases.home.GetHomeExperienceUseCase
import com.katha.mep.mep_ia_poc.usecases.home.GetRecommendationsUseCase
import com.katha.mep.mep_ia_poc.usecases.search.GetSearchSuggestionsUseCase
import com.katha.mep.mep_ia_poc.usecases.search.SearchUseCase
import com.katha.mep.mep_ia_poc.viewmodel.AppViewModel

object AppDi {
    private var config: PocConfig = PocConfig()

    val cacheStore: LocalCacheStore = LocalCacheStore()
    val logger: AiEventLogger = AiEventLogger()
    val metricsCollector: MetricsCollector = MetricsCollector()
    val performanceTracker: PerformanceTracker = PerformanceTracker(logger)
    val resiliencePolicy: ExperienceResiliencePolicy = ExperienceResiliencePolicy()
    val networkMonitor: NetworkMonitor = NetworkMonitor()
    val slaTimer: SlaTimer = SlaTimer(::pocConfig)
    val httpClientProvider: HttpClientProvider = HttpClientProvider(::pocConfig)
    private val sseClient: SseClient = SseClient(httpClientProvider)
    val appViewModel: AppViewModel = AppViewModel(pocConfig(), ::updatePocConfig)
    private val remoteChatService: ChatService = RemoteChatService(sseClient, ::pocConfig)
    private val fakeChatService: ChatService = FakeChatService(::pocConfig)

    val handoffService = FakeHandoffService()
    val emergencySupportService: EmergencySupportService = LocalEmergencySupportService(cacheStore)

    val chatService: ChatService = ChatServiceRouter(
        configProvider = ::pocConfig,
        remoteChatService = remoteChatService,
        fakeChatService = fakeChatService,
    )

    val personalizationService: PersonalizationService
        get() = if (config.useRemoteGateway) {
            RemotePersonalizationService(httpClientProvider, ::pocConfig)
        } else {
            FakePersonalizationService()
        }

    val recommendationService: RecommendationService
        get() = FakeRecommendationService()

    val searchService: SearchService
        get() = if (config.useRemoteGateway) {
            RemoteSearchService(httpClientProvider, ::pocConfig)
        } else {
            FakeSearchService(::pocConfig)
        }

    val sendChatMessageUseCase: SendChatMessageUseCase = SendChatMessageUseCase(chatService)

    val chatExperienceController: ChatExperienceController = ChatExperienceController(
        sendChatMessageUseCase = sendChatMessageUseCase,
        resiliencePolicy = resiliencePolicy,
        performanceTracker = performanceTracker,
        eventLogger = logger,
        metricsCollector = metricsCollector,
        configProvider = ::pocConfig,
    )

    val chatViewModel: ChatViewModel = ChatViewModel(
        appState = appViewModel.state,
        changeScenario = appViewModel::changeScenario,
        changeProvider = appViewModel::changeProvider,
        controller = chatExperienceController,
        handoffService = handoffService,
    )

    val getConversationUseCase: GetConversationUseCase
        get() = GetConversationUseCase(chatService)

    val getHomeExperienceUseCase: GetHomeExperienceUseCase
        get() = GetHomeExperienceUseCase(personalizationService, cacheStore, ::pocConfig)

    val getRecommendationsUseCase: GetRecommendationsUseCase
        get() = GetRecommendationsUseCase(recommendationService)

    val searchUseCase: SearchUseCase
        get() = SearchUseCase(searchService, cacheStore, ::pocConfig)

    val getSearchSuggestionsUseCase: GetSearchSuggestionsUseCase
        get() = GetSearchSuggestionsUseCase(searchService)

    val getEmergencyGuideUseCase: GetEmergencyGuideUseCase =
        GetEmergencyGuideUseCase(emergencySupportService)

    val createEmergencyReportUseCase: CreateEmergencyReportUseCase =
        CreateEmergencyReportUseCase(emergencySupportService)

    val syncPendingEmergencyActionsUseCase: SyncPendingEmergencyActionsUseCase =
        SyncPendingEmergencyActionsUseCase(emergencySupportService)

    fun pocConfig(): PocConfig = config

    fun updatePocConfig(nextConfig: PocConfig) {
        config = nextConfig
        logger.log("pocConfigUpdated", mapOf("config" to nextConfig))
    }
}
