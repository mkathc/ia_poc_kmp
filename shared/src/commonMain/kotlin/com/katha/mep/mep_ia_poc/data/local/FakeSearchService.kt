package com.katha.mep.mep_ia_poc.data.local

import com.katha.mep.mep_ia_poc.config.FakeGatewayScenario
import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.config.SearchProfile
import com.katha.mep.mep_ia_poc.contracts.SearchService
import com.katha.mep.mep_ia_poc.contracts.SearchServiceException
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.search.SearchItem
import com.katha.mep.mep_ia_poc.models.search.SearchResult
import com.katha.mep.mep_ia_poc.models.search.SearchSuggestedAction
import com.katha.mep.mep_ia_poc.models.search.SearchSuggestion
import kotlinx.coroutines.delay

class FakeSearchService(
    private val configProvider: () -> PocConfig = { PocConfig() },
) : SearchService {
    override suspend fun search(
        query: String,
        context: ConversationContext,
        profile: SearchProfile,
    ): SearchResult {
        val config = configProvider()
        when (config.activeScenario) {
            FakeGatewayScenario.delayed -> delay(2_500L)
            FakeGatewayScenario.timeout -> delay(config.slaThresholdMillis + 1_200L)
            FakeGatewayScenario.offline -> throw SearchServiceException("offline", "Service unavailable")
            FakeGatewayScenario.error -> throw SearchServiceException("http_error", "Controlled search error")
            else -> Unit
        }

        if (config.activeScenario == FakeGatewayScenario.noResults) {
            return SearchResult(
                query = query,
                detectedIntent = "unknown",
            )
        }

        val intentType = profile.toIntentType()
        val route = profile.toRoute()
        return SearchResult(
            query = query,
            detectedIntent = intentType,
            results = listOf(
                SearchItem(
                    id = "fake-${profile.profileName}-1",
                    title = profile.titleFor(config.provider.id),
                    description = "Respuesta contextual para \"$query\" usando ${config.provider.id}.",
                    route = route,
                    score = 0.92,
                ),
                SearchItem(
                    id = "fake-${profile.profileName}-2",
                    title = "Acción recomendada",
                    description = "Continuar el flujo asistido de ${profile.journey}.",
                    route = "$route/next",
                    score = 0.84,
                ),
            ),
            suggestedActions = listOf(
                SearchSuggestedAction(
                    id = "open-${profile.profileName}",
                    label = "Abrir",
                    type = "assisted_navigation",
                    payload = mapOf("route" to route),
                ),
            ),
        )
    }

    override suspend fun getSuggestions(
        context: ConversationContext,
        profile: SearchProfile,
    ): List<SearchSuggestion> = defaultSuggestions(profile)
}

private fun SearchProfile.toIntentType(): String =
    when (this) {
        SearchProfile.insurance -> "policy"
        SearchProfile.payments -> "payment"
        SearchProfile.claims -> "claim"
        SearchProfile.benefits -> "benefits"
    }

private fun SearchProfile.toRoute(): String =
    when (this) {
        SearchProfile.insurance -> "/policy"
        SearchProfile.payments -> "/payments"
        SearchProfile.claims -> "/claims"
        SearchProfile.benefits -> "/benefits"
    }

private fun SearchProfile.titleFor(providerId: String): String =
    when (this) {
        SearchProfile.insurance -> "Cobertura y póliza"
        SearchProfile.payments -> "Pagos pendientes"
        SearchProfile.claims -> "Reporte de siniestro"
        SearchProfile.benefits -> "Beneficios disponibles"
    } + " ($providerId)"

private fun defaultSuggestions(profile: SearchProfile): List<SearchSuggestion> =
    when (profile) {
        SearchProfile.insurance -> listOf(
            SearchSuggestion("s-coverage", "Qué cubre mi seguro"),
            SearchSuggestion("s-policy", "Ver mi póliza"),
        )
        SearchProfile.payments -> listOf(
            SearchSuggestion("s-pay", "Cómo pago mi cuota"),
            SearchSuggestion("s-debt", "Tengo un pago pendiente"),
        )
        SearchProfile.claims -> listOf(
            SearchSuggestion("s-claim", "Cómo reporto un siniestro"),
            SearchSuggestion("s-status", "Estado de mi reclamo"),
        )
        SearchProfile.benefits -> listOf(
            SearchSuggestion("s-benefits", "Qué beneficios tengo"),
            SearchSuggestion("s-discounts", "Descuentos disponibles"),
        )
    }
