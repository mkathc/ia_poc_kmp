package com.katha.mep.mep_ia_poc.viewmodel

import com.katha.mep.mep_ia_poc.chat.ChatExperienceController
import com.katha.mep.mep_ia_poc.chat.ChatViewModel
import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.data.local.FakeChatService
import com.katha.mep.mep_ia_poc.data.local.FakeHandoffService
import com.katha.mep.mep_ia_poc.observability.AiEventLogger
import com.katha.mep.mep_ia_poc.observability.MetricsCollector
import com.katha.mep.mep_ia_poc.observability.PerformanceTracker
import com.katha.mep.mep_ia_poc.resilience.ExperienceResiliencePolicy
import com.katha.mep.mep_ia_poc.usecases.chat.SendChatMessageUseCase

fun previewAppViewModel(): AppViewModel =
    AppViewModel(
        initialConfig = PocConfig(),
        updatePocConfig = {},
    )

fun previewChatViewModel(appViewModel: AppViewModel): ChatViewModel {
    val logger = AiEventLogger()
    val config = PocConfig()
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
    )
}
