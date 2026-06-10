package com.katha.mep.mep_ia_poc.data.remote

import com.katha.mep.mep_ia_poc.config.MockUserProfile
import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.contracts.PersonalizationService
import com.katha.mep.mep_ia_poc.contracts.PersonalizationServiceException
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.home.ActionSuggestion
import com.katha.mep.mep_ia_poc.models.home.HomeAlert
import com.katha.mep.mep_ia_poc.models.home.HomeCard
import com.katha.mep.mep_ia_poc.models.home.HomeExperience
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

class RemotePersonalizationService(
    private val httpClientProvider: HttpClientProvider,
    private val configProvider: () -> PocConfig,
) : PersonalizationService {
    override suspend fun getHomeExperience(
        context: ConversationContext,
        profile: MockUserProfile,
    ): HomeExperience {
        val config = configProvider()
        val scenario = config.activeScenario.wireName
        val provider = config.provider.id
        val baseUrl = config.remoteGatewayBaseUrl.trimEnd('/')
        val url = "$baseUrl/home/personalization?scenario=$scenario&provider=$provider"
        val homeContext = profile.toHomeRequestContext()
        val body = homeRequestBody(userId = context.userId, context = homeContext)

        println("[home] request data={scenario=$scenario, provider=$provider, profile=${profile.profileName}, baseUrl=$baseUrl}")
        println(
            "[home] generated context data={hasActiveClaim=${homeContext.hasActiveClaim}, " +
                "hasPendingPayment=${homeContext.hasPendingPayment}, lastInteraction=${homeContext.lastInteraction}}",
        )

        val response = try {
            httpClientProvider.client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        } catch (exception: Throwable) {
            throw PersonalizationServiceException("network_error", exception.message ?: "Network error", exception)
        }

        println("[HTTP][home] status=${response.status.value} scenario=$scenario provider=$provider profile=${profile.profileName}")

        if (response.status == HttpStatusCode.ServiceUnavailable) {
            throw PersonalizationServiceException("offline", "Service unavailable")
        }
        if (!response.status.isSuccess()) {
            throw PersonalizationServiceException("http_error", "HTTP ${response.status.value}")
        }

        return try {
            response.bodyAsText().toHomeExperience()
        } catch (exception: Throwable) {
            throw PersonalizationServiceException("invalid_response", "Invalid home response", exception)
        }
    }

    override suspend fun refreshHomeExperience(
        context: ConversationContext,
        profile: MockUserProfile,
    ): HomeExperience = getHomeExperience(context, profile)
}

private data class HomeRequestContext(
    val hasActiveClaim: Boolean,
    val hasPendingPayment: Boolean,
    val lastInteraction: String,
)

private fun MockUserProfile.toHomeRequestContext(): HomeRequestContext =
    when (this) {
        MockUserProfile.activeClaim -> HomeRequestContext(true, false, "claim_status")
        MockUserProfile.pendingPayment -> HomeRequestContext(false, true, "payment")
        MockUserProfile.newUser -> HomeRequestContext(false, false, "first_open")
        MockUserProfile.benefitsFocused -> HomeRequestContext(false, false, "benefits")
    }

private fun homeRequestBody(
    userId: String,
    context: HomeRequestContext,
): String =
    buildJsonObject {
        put("userId", userId)
        put(
            "context",
            buildJsonObject {
                put("hasActiveClaim", context.hasActiveClaim)
                put("hasPendingPayment", context.hasPendingPayment)
                put("lastInteraction", context.lastInteraction)
                put("locale", "es-PE")
            },
        )
    }.toString()

private fun String.toHomeExperience(): HomeExperience {
    val root = Json.parseToJsonElement(this).jsonObject
    return HomeExperience(
        userSegment = root.stringValue("userSegment").ifBlank { "unknown" },
        cards = root.arrayValue("cards").mapNotNull { it.asObjectOrNull()?.toHomeCard() }.sortedBy { it.priority },
        alerts = root.arrayValue("alerts").mapNotNull { it.asObjectOrNull()?.toHomeAlert() },
        nextBestActions = root.arrayValue("nextBestActions").mapNotNull { it.asObjectOrNull()?.toActionSuggestion() },
        isFromCache = false,
        generatedAt = "local-now",
    )
}

private fun JsonObject.toHomeCard(): HomeCard =
    HomeCard(
        id = stringValue("id").ifBlank { "card-${hashCode()}" },
        type = stringValue("type").ifBlank { "unknown" },
        title = stringValue("title"),
        description = stringValue("description"),
        priority = intValue("priority") ?: 0,
        action = objectValue("action")?.toActionSuggestion(),
    )

private fun JsonObject.toHomeAlert(): HomeAlert =
    HomeAlert(
        id = stringValue("id").ifBlank { "alert-${hashCode()}" },
        title = stringValue("title"),
        message = stringValue("message"),
        severity = stringValue("severity").ifBlank { "info" },
    )

private fun JsonObject.toActionSuggestion(): ActionSuggestion =
    ActionSuggestion(
        id = stringValue("id").ifBlank { "action-${hashCode()}" },
        label = stringValue("label"),
        route = stringValue("route"),
        type = stringValue("type").ifBlank { null },
        payload = objectValue("payload")?.toPrimitiveMap().orEmpty(),
    )

private fun JsonObject.toPrimitiveMap(): Map<String, Any?> =
    entries.associate { (key, value) ->
        key to when (value) {
            is JsonPrimitive -> value.booleanOrNull ?: value.intOrNull ?: value.doubleOrNull ?: value.contentOrNull
            is JsonObject -> value.toPrimitiveMap()
            is JsonArray -> value.map { element -> (element as? JsonPrimitive)?.contentOrNull ?: element.toString() }
        }
    }

private fun JsonObject.arrayValue(name: String): List<JsonElement> =
    (this[name] as? JsonArray)?.jsonArray.orEmpty()

private fun JsonObject.objectValue(name: String): JsonObject? =
    this[name]?.asObjectOrNull()

private fun JsonElement.asObjectOrNull(): JsonObject? =
    this as? JsonObject

private fun JsonObject.stringValue(name: String): String =
    (this[name] as? JsonPrimitive)?.jsonPrimitive?.contentOrNull.orEmpty()

private fun JsonObject.intValue(name: String): Int? =
    (this[name] as? JsonPrimitive)?.jsonPrimitive?.intOrNull
