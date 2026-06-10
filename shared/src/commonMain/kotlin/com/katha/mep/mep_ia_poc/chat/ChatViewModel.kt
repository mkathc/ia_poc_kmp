package com.katha.mep.mep_ia_poc.chat

import com.katha.mep.mep_ia_poc.config.FakeGatewayScenario
import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.config.ProviderMock
import com.katha.mep.mep_ia_poc.contracts.HandoffService
import com.katha.mep.mep_ia_poc.models.chat.ChatMessage
import com.katha.mep.mep_ia_poc.models.chat.ChatRequest
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyAnswer
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyContact
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyConversationStep
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyGuide
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyType
import com.katha.mep.mep_ia_poc.models.localai.LocalAiCapabilityType
import com.katha.mep.mep_ia_poc.models.localai.LocalAiRequest
import com.katha.mep.mep_ia_poc.models.resilience.ExperienceContext
import com.katha.mep.mep_ia_poc.models.resilience.ExperienceFallbackType
import com.katha.mep.mep_ia_poc.observability.AiEventLogger
import com.katha.mep.mep_ia_poc.observability.MetricsCollector
import com.katha.mep.mep_ia_poc.observability.PerformanceTracker
import com.katha.mep.mep_ia_poc.state.PocDebugState
import com.katha.mep.mep_ia_poc.usecases.emergency.CreateEmergencyReportUseCase
import com.katha.mep.mep_ia_poc.usecases.emergency.GetEmergencyGuideUseCase
import com.katha.mep.mep_ia_poc.usecases.emergency.GetPendingEmergencyReportsUseCase
import com.katha.mep.mep_ia_poc.usecases.emergency.SyncPendingEmergencyActionsUseCase
import com.katha.mep.mep_ia_poc.usecases.localai.ExecuteLocalAiCapabilityUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

class ChatViewModel(
    private val appState: StateFlow<PocDebugState>,
    private val changeScenario: (FakeGatewayScenario) -> Unit,
    private val changeProvider: (ProviderMock) -> Unit,
    private val controller: ChatExperienceController,
    private val handoffService: HandoffService,
    private val getEmergencyGuideUseCase: GetEmergencyGuideUseCase,
    private val createEmergencyReportUseCase: CreateEmergencyReportUseCase,
    private val getPendingEmergencyReportsUseCase: GetPendingEmergencyReportsUseCase,
    private val syncPendingEmergencyActionsUseCase: SyncPendingEmergencyActionsUseCase,
    private val executeLocalAiCapabilityUseCase: ExecuteLocalAiCapabilityUseCase,
    private val performanceTracker: PerformanceTracker,
    private val eventLogger: AiEventLogger,
    private val metricsCollector: MetricsCollector,
    private val configProvider: () -> PocConfig,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val remoteToLocalAssistantIds = mutableMapOf<String, String>()
    private val mutableState = MutableStateFlow(
        ChatState(
            activeScenario = appState.value.activeScenario,
            activeProvider = appState.value.activeProvider,
        ),
    )
    val state: StateFlow<ChatState> = mutableState.asStateFlow()

    init {
        scope.launch {
            appState.collect { debugState ->
                mutableState.update { current ->
                    val next = current.copy(
                        activeScenario = debugState.activeScenario,
                        activeProvider = debugState.activeProvider,
                    )
                    if (debugState.activeScenario.isOnlineSuccessScenario()) {
                        next.clearEmergencyInlineState("scenario_online")
                    } else {
                        next
                    }
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return

        if (state.value.activeScenario.isOnlineSuccessScenario()) {
            clearEmergencyInlineFlow("cloud_retry")
        }

        val userMessage = ChatMessage(
            id = "local-user-${state.value.messages.size + 1}",
            text = cleanText,
            isUser = true,
            createdAt = "local-now",
        )

        mutableState.update {
            it.copy(
                messages = it.messages + userMessage,
                isStreaming = true,
                errorMessage = null,
                lastUserMessage = cleanText,
            )
        }

        val request = ChatRequest(
            conversationId = ConversationId,
            message = cleanText,
            context = ConversationContext(
                userId = "poc-user",
                channel = "kmp",
                journey = "chat-streaming-poc",
            ),
        )

        scope.launch {
            controller.sendMessage(request).collect { event ->
                reduce(event)
            }
        }
    }

    fun changeScenario(scenario: FakeGatewayScenario) {
        changeScenario.invoke(scenario)
        if (scenario.isOnlineSuccessScenario()) {
            clearEmergencyInlineFlow("scenario_online")
        }
    }

    fun changeProvider(provider: ProviderMock) {
        changeProvider.invoke(provider)
    }

    fun retryLastMessage() {
        state.value.lastUserMessage?.let(::sendMessage)
    }

    fun retryCloudAssistant() {
        println("[emergency] retry cloud assistant")
        retryLastMessage()
    }

    fun dismissFallback() {
        mutableState.update { it.copy(activeFallback = null, hasSlaViolation = false) }
    }

    fun openWhatsappFallback() {
        scope.launch {
            val current = state.value
            val fallback = handoffService.openWhatsappFallback(
                ExperienceContext(
                    feature = "chat",
                    userId = "poc-user",
                    providerId = current.activeProvider.id,
                    scenario = current.activeScenario.wireName,
                ),
            )
            mutableState.update { it.copy(activeFallback = fallback) }
        }
    }

    fun startOfflineEmergencySupport(reason: String) {
        if (state.value.showEmergencyOptions && state.value.emergencyMessage == OfflineSupportIntro) return

        println("[emergency] inline flow started reason=$reason")
        eventLogger.log("emergencyInlineFlowStarted", mapOf("reason" to reason))
        mutableState.update {
            it.copy(
                messages = it.messages + localAssistantMessage(OfflineSupportIntro),
                showEmergencyOptions = true,
                activeFallback = null,
                errorMessage = null,
                emergencyMessage = OfflineSupportIntro,
            )
        }
    }

    fun selectEmergencyType(type: EmergencyType) {
        println("[emergency] emergency type selected type=${type.name}")
        eventLogger.log("emergencyTypeSelected", mapOf("type" to type.name))
        mutableState.update {
            it.copy(
                messages = it.messages + localUserMessage(type.displayName),
                showEmergencyOptions = false,
                selectedEmergencyType = type,
                activeEmergencyGuide = null,
                emergencyAnswers = emptyList(),
                emergencyConversationCompleted = false,
                emergencyMessage = null,
            )
        }

        scope.launch {
            val mark = TimeSource.Monotonic.markNow()
            println("[performance] emergencyGuideLoadStart")
            performanceTracker.emergencyGuideLoadStart()
            val guide = getEmergencyGuideUseCase(type)
            val elapsed = mark.elapsedNow().inWholeMilliseconds
            println("[performance] emergencyGuideLoadCompleted timeToEmergencyGuide=$elapsed")
            println("[emergency] local guide loaded type=${type.name}")
            performanceTracker.emergencyGuideLoadCompleted(elapsed)
            performanceTracker.emergencyGuideLoadTime(elapsed)
            metricsCollector.record("emergencyGuideLoad", mapOf("type" to type.name, "timeToEmergencyGuide" to elapsed))
            eventLogger.log("emergencyLocalGuideLoaded", mapOf("type" to type.name))

            mutableState.update {
                it.copy(
                    activeEmergencyGuide = guide,
                    messages = it.messages + localAssistantMessage(guide.toChatText()),
                )
            }
            displayEmergencyQuestion(EmergencySteps.first())
        }
    }

    fun answerEmergencyQuestion(questionId: String, answer: String) {
        val currentStep = state.value.currentEmergencyStep ?: return
        if (currentStep.id != questionId) return

        println("[emergency] answer selected questionId=$questionId answer=$answer")
        eventLogger.log("emergencyAnswerSelected", mapOf("questionId" to questionId, "answer" to answer))
        val emergencyAnswer = EmergencyAnswer(
            questionId = currentStep.id,
            question = currentStep.question,
            answer = answer,
        )

        mutableState.update {
            it.copy(
                messages = it.messages + localUserMessage(answer),
                emergencyAnswers = it.emergencyAnswers + emergencyAnswer,
            )
        }

        val nextStep = EmergencySteps.firstOrNull { it.order == currentStep.order + 1 }
        if (nextStep != null) {
            displayEmergencyQuestion(nextStep)
            return
        }

        println("[emergency] conversation completed")
        eventLogger.log("emergencyConversationCompleted")
        mutableState.update { it.copy(currentEmergencyStep = null, emergencyConversationCompleted = true) }
        executeLocalAiReadinessIfEnabled()
        if (answer == "Sí") {
            saveEmergencyReport()
        } else {
            mutableState.update {
                it.copy(
                    messages = it.messages + localAssistantMessage(
                        "Puedes continuar utilizando la guía offline mientras recuperas conexión.",
                    ),
                )
            }
        }
    }

    fun saveEmergencyReport() {
        val type = state.value.selectedEmergencyType ?: return
        val answers = state.value.emergencyAnswers
        scope.launch {
            val report = createEmergencyReportUseCase(type, answers)
            println("[emergency] local report created id=${report.id}")
            eventLogger.log("emergencyLocalReportCreated", mapOf("id" to report.id, "type" to type.name))
            metricsCollector.record("emergencyReportCreated", mapOf("type" to type.name))
            val pendingReports = getPendingEmergencyReportsUseCase()
            mutableState.update {
                it.copy(
                    pendingEmergencyReports = pendingReports,
                    messages = it.messages + listOf(
                        localAssistantMessage("Tu reporte fue guardado localmente."),
                        localAssistantMessage("Se sincronizará automáticamente cuando vuelva la conexión."),
                    ),
                )
            }
        }
    }

    fun syncPendingEmergencyReports() {
        scope.launch {
            println("[emergency] sync started")
            eventLogger.log("emergencySyncStarted")
            performanceTracker.emergencySyncStart()
            mutableState.update { it.copy(isEmergencySyncing = true) }
            syncPendingEmergencyActionsUseCase()
            println("[emergency] sync completed")
            eventLogger.log("emergencySyncCompleted")
            performanceTracker.emergencySyncCompleted()
            metricsCollector.record("emergencySyncCompleted")
            val pendingReports = getPendingEmergencyReportsUseCase()
            mutableState.update {
                it.copy(
                    isEmergencySyncing = false,
                    pendingEmergencyReports = pendingReports,
                    messages = it.messages + localAssistantMessage("Tu reporte fue sincronizado correctamente."),
                )
            }
        }
    }

    fun tapEmergencyContact(contact: EmergencyContact) {
        println("[emergency] contact tapped phone=${contact.phone}")
        eventLogger.log("emergencyContactTapped", mapOf("phone" to contact.phone, "label" to contact.label))
    }

    private fun reduce(event: ChatExperienceEvent) {
        when (event) {
            is ChatExperienceEvent.MessageStarted -> {
                val localMessageId = nextAssistantMessageId(event.messageId)
                remoteToLocalAssistantIds[event.messageId] = localMessageId
                val assistantMessage = ChatMessage(
                    id = localMessageId,
                    text = "",
                    isUser = false,
                    createdAt = "local-now",
                    isStreaming = true,
                )
                mutableState.update { it.copy(messages = it.messages + assistantMessage, isStreaming = true) }
            }
            is ChatExperienceEvent.MessageDelta -> {
                val localMessageId = remoteToLocalAssistantIds.getOrPut(event.messageId) {
                    nextAssistantMessageId(event.messageId)
                }
                mutableState.update { state ->
                    state.copy(
                        messages = state.messages.upsertAssistantMessage(localMessageId) {
                            it.copy(text = event.text, isStreaming = true)
                        },
                        isStreaming = true,
                    )
                }
            }
            is ChatExperienceEvent.MessageCompleted -> {
                val localMessageId = remoteToLocalAssistantIds.getOrPut(event.messageId) {
                    nextAssistantMessageId(event.messageId)
                }
                mutableState.update { state ->
                    val next = state.copy(
                        messages = state.messages.upsertAssistantMessage(localMessageId) {
                            it.copy(text = event.text, isStreaming = false)
                        },
                        isStreaming = false,
                    )
                    if (state.activeScenario.isOnlineSuccessScenario()) {
                        next.clearEmergencyInlineState("online_restored")
                    } else {
                        next
                    }
                }
            }
            is ChatExperienceEvent.FallbackTriggered -> {
                println("[ChatViewModel] fallback consolidated type=${event.fallback.type} reason=${event.fallback.reason}")
                mutableState.update {
                    it.copy(
                        activeFallback = event.fallback,
                        hasSlaViolation = event.fallback.type == ExperienceFallbackType.SlaTimeout || it.hasSlaViolation,
                        errorMessage = null,
                    )
                }
            }
            is ChatExperienceEvent.Error -> {
                println("[ChatViewModel] stream stopped reason=${event.code}")
                mutableState.update { state ->
                    val cleanedMessages = state.messages.stopStreamingAndRemoveEmptyAssistantPlaceholders(event.code)
                    if (cleanedMessages.size != state.messages.size) {
                        println("[ChatViewModel] placeholder removed reason=${event.code}")
                    }
                    val consolidatedError = if (state.activeFallback == null) {
                        event.toUserFacingError()
                    } else {
                        null
                    }
                    state.copy(
                        isStreaming = false,
                        errorMessage = consolidatedError,
                        messages = cleanedMessages,
                    )
                }
                when (event.code) {
                    "offline", "connectionLost" -> startOfflineEmergencySupport(event.code)
                }
            }
        }
    }

    private fun clearEmergencyInlineFlow(reason: String) {
        mutableState.update { it.clearEmergencyInlineState(reason) }
    }

    private fun ChatState.clearEmergencyInlineState(reason: String): ChatState {
        if (!hasActiveEmergencyInlineState()) return this

        println("[emergency] inline flow cleared reason=$reason")
        if (reason == "scenario_online" || reason == "online_restored") {
            println("[emergency] inline actions hidden reason=online_restored")
        }
        eventLogger.log("emergencyInlineFlowCleared", mapOf("reason" to reason))

        return copy(
            showEmergencyOptions = false,
            selectedEmergencyType = null,
            activeEmergencyGuide = null,
            emergencyAnswers = emptyList(),
            currentEmergencyStep = null,
            emergencyConversationCompleted = false,
            isEmergencySyncing = false,
            emergencyMessage = null,
            localAiResult = null,
            activeFallback = null,
            hasSlaViolation = false,
            errorMessage = null,
        )
    }

    private fun ChatState.hasActiveEmergencyInlineState(): Boolean =
        showEmergencyOptions ||
            selectedEmergencyType != null ||
            activeEmergencyGuide != null ||
            emergencyAnswers.isNotEmpty() ||
            currentEmergencyStep != null ||
            emergencyConversationCompleted ||
            isEmergencySyncing ||
            emergencyMessage != null ||
            activeFallback != null ||
            errorMessage != null

    private fun displayEmergencyQuestion(step: EmergencyConversationStep) {
        println("[emergency] question displayed id=${step.id}")
        eventLogger.log("emergencyQuestionDisplayed", mapOf("questionId" to step.id))
        mutableState.update {
            it.copy(
                currentEmergencyStep = step,
                messages = it.messages + localAssistantMessage(step.question),
            )
        }
    }

    private fun executeLocalAiReadinessIfEnabled() {
        val current = state.value
        val type = current.selectedEmergencyType ?: return
        if (!configProvider().enableLocalAiReadiness) return

        scope.launch {
            val result = executeLocalAiCapabilityUseCase(
                LocalAiRequest(
                    capabilityType = LocalAiCapabilityType.emergencyAssessment,
                    emergencyType = type,
                    answers = state.value.emergencyAnswers,
                ),
            )
            mutableState.update {
                it.copy(
                    localAiResult = result,
                    messages = it.messages + localAssistantMessage(
                        "Resumen generado: ${result.summary}\n" +
                            "Siguiente acción recomendada: ${result.recommendedNextStep}\n" +
                            "Confianza estimada: ${(result.confidence * 100).toInt()}%",
                    ),
                )
            }
        }
    }

    private fun localAssistantMessage(text: String): ChatMessage =
        ChatMessage(
            id = "local-assistant-${state.value.messages.size + 1}-${text.hashCode()}",
            text = text,
            isUser = false,
            createdAt = "local-now",
            isStreaming = false,
        )

    private fun localUserMessage(text: String): ChatMessage =
        ChatMessage(
            id = "local-user-${state.value.messages.size + 1}-${text.hashCode()}",
            text = text,
            isUser = true,
            createdAt = "local-now",
            isStreaming = false,
        )

    private fun List<ChatMessage>.replaceMessage(
        messageId: String,
        transform: (ChatMessage) -> ChatMessage,
    ): List<ChatMessage> =
        map { message -> if (message.id == messageId) transform(message) else message }

    private fun List<ChatMessage>.upsertAssistantMessage(
        messageId: String,
        transform: (ChatMessage) -> ChatMessage,
    ): List<ChatMessage> {
        val existing = firstOrNull { it.id == messageId }
        return if (existing == null) {
            this + transform(
                ChatMessage(
                    id = messageId,
                    text = "",
                    isUser = false,
                    createdAt = "local-now",
                    isStreaming = true,
                ),
            )
        } else {
            replaceMessage(messageId, transform)
        }
    }

    private fun nextAssistantMessageId(remoteMessageId: String): String =
        "assistant-${state.value.messages.size + 1}-$remoteMessageId"

    private fun List<ChatMessage>.stopStreamingAndRemoveEmptyAssistantPlaceholders(
        errorCode: String?,
    ): List<ChatMessage> =
        mapNotNull { message ->
            when {
                message.isUser -> message
                message.isStreaming && message.text.isBlank() -> null
                message.isStreaming -> message.copy(isStreaming = false)
                else -> message
            }
        }

    private fun ChatExperienceEvent.Error.toUserFacingError(): String =
        when (code) {
            "offline" -> "No tengo conexión con el asistente cloud por el momento."
            "connectionLost" -> "Se perdió la conexión con el asistente. Puedes reintentar."
            else -> "Ocurrió un error con el asistente. Puedes reintentar."
        }

    private companion object {
        const val ConversationId = "poc-chat-conversation"
        const val OfflineSupportIntro =
            "No tengo conexión con el asistente cloud por el momento, pero puedo orientarte con soporte offline. ¿Qué tipo de emergencia necesitas?"

        val EmergencySteps = listOf(
            EmergencyConversationStep(
                id = "safe_zone",
                question = "¿Te encuentras en una zona segura?",
                options = listOf("Sí", "No"),
                order = 1,
            ),
            EmergencyConversationStep(
                id = "injured_people",
                question = "¿Hay personas heridas?",
                options = listOf("Sí", "No", "No estoy seguro"),
                order = 2,
            ),
            EmergencyConversationStep(
                id = "register_report",
                question = "¿Deseas registrar un reporte para sincronizarlo más tarde?",
                options = listOf("Sí", "No"),
                order = 3,
            ),
        )
    }
}

private fun EmergencyGuide.toChatText(): String =
    buildString {
        appendLine(title)
        appendLine()
        steps.forEachIndexed { index, step ->
            appendLine("${index + 1}. $step")
        }
        appendLine()
        appendLine("Contactos:")
        contacts.forEach { contact ->
            appendLine("- ${contact.label}: ${contact.phone} (${contact.available})")
        }
        appendLine()
        append("Disponible offline")
    }

private fun FakeGatewayScenario.isOnlineSuccessScenario(): Boolean =
    this == FakeGatewayScenario.happyPath ||
        this == FakeGatewayScenario.delayed ||
        this == FakeGatewayScenario.longStream
