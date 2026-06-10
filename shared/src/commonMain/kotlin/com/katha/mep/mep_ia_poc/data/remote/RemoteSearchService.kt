package com.katha.mep.mep_ia_poc.data.remote

import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.config.SearchProfile
import com.katha.mep.mep_ia_poc.contracts.SearchService
import com.katha.mep.mep_ia_poc.contracts.SearchServiceException
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.search.SearchItem
import com.katha.mep.mep_ia_poc.models.search.SearchResult
import com.katha.mep.mep_ia_poc.models.search.SearchSuggestedAction
import com.katha.mep.mep_ia_poc.models.search.SearchSuggestion
import com.katha.mep.mep_ia_poc.network.HttpClientProvider
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class RemoteSearchService(
    private val httpClientProvider: HttpClientProvider,
    private val configProvider: () -> PocConfig,
) : SearchService {
    override suspend fun search(
        query: String,
        context: ConversationContext,
        profile: SearchProfile,
    ): SearchResult {
        val config = configProvider()
        val scenario = config.activeScenario.wireName
        val provider = config.provider.id
        val baseUrl = config.remoteGatewayBaseUrl.trimEnd('/')
        val url = "$baseUrl/search?scenario=$scenario&provider=$provider"
        val body = searchRequestBody(query = query, userId = context.userId, profile = profile)

        println("[search] request data={scenario=$scenario, provider=$provider, profile=${profile.profileName}, query=$query, baseUrl=$baseUrl}")

        val response = try {
            httpClientProvider.client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        } catch (exception: Throwable) {
            throw SearchServiceException("network_error", exception.message ?: "Network error", exception)
        }

        println("[HTTP][search] status=${response.status.value} scenario=$scenario provider=$provider profile=${profile.profileName}")

        if (response.status == HttpStatusCode.ServiceUnavailable) {
            throw SearchServiceException("offline", "Service unavailable")
        }
        if (!response.status.isSuccess()) {
            throw SearchServiceException("http_error", "HTTP ${response.status.value}")
        }

        return try {
            val mapped = response.bodyAsText().toSearchResult(query)
            println(
                "[search] mapped intent=${mapped.detectedIntent} " +
                    "results=${mapped.results.size} suggestedActions=${mapped.suggestedActions.size}",
            )
            mapped
        } catch (exception: Throwable) {
            throw SearchServiceException("invalid_response", "Invalid search response", exception)
        }
    }

    override suspend fun getSuggestions(
        context: ConversationContext,
        profile: SearchProfile,
    ): List<SearchSuggestion> =
        when (profile) {
            SearchProfile.insurance -> listOf(SearchSuggestion("remote-insurance", "Qué cubre mi seguro"))
            SearchProfile.payments -> listOf(SearchSuggestion("remote-payments", "Cómo pago mi cuota"))
            SearchProfile.claims -> listOf(SearchSuggestion("remote-claims", "Cómo reporto un siniestro"))
            SearchProfile.benefits -> listOf(SearchSuggestion("remote-benefits", "Qué beneficios tengo"))
        }
}

private fun searchRequestBody(
    query: String,
    userId: String,
    profile: SearchProfile,
): String =
    buildJsonObject {
        put("query", query)
        put("userId", userId)
        put(
            "context",
            buildJsonObject {
                put("locale", "es-PE")
                put("lastInteraction", profile.lastInteraction())
            },
        )
    }.toString()

private fun SearchProfile.lastInteraction(): String =
    when (this) {
        SearchProfile.insurance -> "policy"
        SearchProfile.payments -> "payment"
        SearchProfile.claims -> "claim_status"
        SearchProfile.benefits -> "benefits"
    }

private fun String.toSearchResult(fallbackQuery: String): SearchResult {
    val root = Json.parseToJsonElement(this).jsonObject
    val detectedIntent = root.intentValue()
    val actions = root.arrayValue("suggestedActions", "actions")
        .mapNotNull { it.asObjectOrNull()?.toSearchSuggestedAction() }

    return SearchResult(
        query = root.stringValue("query").ifBlank { fallbackQuery },
        detectedIntent = detectedIntent,
        results = root.arrayValue("results", "items")
            .mapNotNull { it.asObjectOrNull()?.toSearchItem() },
        suggestedActions = actions,
        isFromCache = false,
    )
}

private fun JsonObject.toSearchItem(): SearchItem =
    SearchItem(
        id = stringValue("id").ifBlank { "result-${hashCode()}" },
        title = stringValue("title"),
        description = stringValue("description"),
        route = stringValue("route"),
        score = doubleValue("score") ?: 0.0,
    )

private fun JsonObject.toSearchSuggestion(): SearchSuggestion =
    SearchSuggestion(
        id = stringValue("id").ifBlank { "suggestion-${hashCode()}" },
        text = stringValue("text").ifBlank { stringValue("label") },
    )

private fun JsonObject.toSearchSuggestedAction(): SearchSuggestedAction {
    val route = stringValue("route")
    val payload = objectValue("payload")?.toPrimitiveMap().orEmpty().ifEmpty {
        if (route.isNotBlank()) mapOf("route" to route) else emptyMap()
    }
    return SearchSuggestedAction(
        id = stringValue("id").ifBlank { "action-${hashCode()}" },
        label = stringValue("label").ifBlank { stringValue("title") },
        type = stringValue("type").ifBlank { "assisted_navigation" },
        payload = payload,
    )
}

private fun JsonObject.toPrimitiveMap(): Map<String, Any?> =
    entries.associate { (key, value) ->
        key to when (value) {
            is JsonPrimitive -> value.booleanOrNull ?: value.intOrNull ?: value.doubleOrNull ?: value.contentOrNull
            is JsonObject -> value.toPrimitiveMap()
            is JsonArray -> value.map { element -> (element as? JsonPrimitive)?.contentOrNull ?: element.toString() }
        }
    }

private fun JsonObject.arrayValue(vararg names: String): List<JsonElement> =
    names.firstNotNullOfOrNull { name -> (this[name] as? JsonArray)?.jsonArray }.orEmpty()

private fun JsonObject.objectValue(name: String): JsonObject? =
    this[name]?.asObjectOrNull()

private fun JsonObject.intentValue(): String {
    val detectedIntent = stringValue("detectedIntent")
    if (detectedIntent.isNotBlank()) return detectedIntent

    val intentElement = this["intent"]
    val intentObject = intentElement as? JsonObject
    if (intentObject != null) {
        return intentObject.stringValue("type")
            .ifBlank { intentObject.stringValue("id") }
            .ifBlank { "unknown" }
    }

    return (intentElement as? JsonPrimitive)?.jsonPrimitive?.contentOrNull?.ifBlank { null } ?: "unknown"
}

private fun JsonElement.asObjectOrNull(): JsonObject? =
    this as? JsonObject

private fun JsonObject.stringValue(name: String): String =
    (this[name] as? JsonPrimitive)?.jsonPrimitive?.contentOrNull.orEmpty()

private fun JsonObject.doubleValue(name: String): Double? =
    (this[name] as? JsonPrimitive)?.jsonPrimitive?.doubleOrNull
