package com.katha.mep.mep_ia_poc.data.local

import com.katha.mep.mep_ia_poc.config.FakeGatewayScenario
import com.katha.mep.mep_ia_poc.config.MockUserProfile
import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.contracts.PersonalizationService
import com.katha.mep.mep_ia_poc.contracts.PersonalizationServiceException
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.home.ActionSuggestion
import com.katha.mep.mep_ia_poc.models.home.HomeAlert
import com.katha.mep.mep_ia_poc.models.home.HomeCard
import com.katha.mep.mep_ia_poc.models.home.HomeExperience
import kotlinx.coroutines.delay

class FakePersonalizationService(
    private val configProvider: () -> PocConfig = { PocConfig() },
) : PersonalizationService {
    override suspend fun getHomeExperience(
        context: ConversationContext,
        profile: MockUserProfile,
    ): HomeExperience {
        val config = configProvider()
        when (config.activeScenario) {
            FakeGatewayScenario.delayed -> delay(2_500)
            FakeGatewayScenario.timeout -> delay(config.slaThresholdMillis + 700)
            FakeGatewayScenario.offline -> throw PersonalizationServiceException("offline", "Service unavailable")
            FakeGatewayScenario.error -> throw PersonalizationServiceException("http_error", "Controlled error")
            else -> Unit
        }
        return buildExperience(profile, config.provider.id)
    }

    override suspend fun refreshHomeExperience(
        context: ConversationContext,
        profile: MockUserProfile,
    ): HomeExperience = getHomeExperience(context, profile)

    private fun buildExperience(profile: MockUserProfile, providerId: String): HomeExperience =
        HomeExperience(
            userSegment = profile.profileName,
            cards = listOf(
                HomeCard(
                    id = "base-card-${profile.profileName}",
                    type = "personalized",
                    title = "Experiencia ${profile.profileName}",
                    description = "Contenido personalizado desde $providerId.",
                    priority = 1,
                    action = ActionSuggestion("base-action", "Ver detalle", "/home/detail"),
                ),
            ),
            alerts = if (profile == MockUserProfile.pendingPayment) {
                listOf(HomeAlert("payment-alert", "Pago pendiente", "Tienes una accion pendiente.", "warning"))
            } else {
                emptyList()
            },
            nextBestActions = listOf(ActionSuggestion("continue", "Continuar", "/next")),
            generatedAt = "local-now",
        )
}
