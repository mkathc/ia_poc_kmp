package com.katha.mep.mep_ia_poc.usecases.search

import com.katha.mep.mep_ia_poc.cache.LocalCacheStore
import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.config.SearchProfile
import com.katha.mep.mep_ia_poc.contracts.SearchService
import com.katha.mep.mep_ia_poc.contracts.SearchServiceException
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.search.SearchResult

class SearchUseCase(
    private val searchServiceProvider: () -> SearchService,
    private val cacheStore: LocalCacheStore,
    private val configProvider: () -> PocConfig,
) {
    suspend operator fun invoke(
        query: String,
        context: ConversationContext,
        profile: SearchProfile,
    ): SearchUseCaseResult {
        val config = configProvider()
        val normalizedQuery = normalizeQuery(query)
        val cacheKey = LocalCacheStore.searchKey(config.provider.id, profile.profileName, normalizedQuery)

        return try {
            val result = searchServiceProvider().search(query, context, profile)
            println("[search] cache save key=$cacheKey")
            cacheStore.save(cacheKey, result)
            SearchUseCaseResult.Success(result)
        } catch (exception: SearchServiceException) {
            cachedOrError(cacheKey, exception.code, exception.message.orEmpty())
        } catch (exception: Throwable) {
            cachedOrError(cacheKey, "unknown_error", exception.message.orEmpty())
        }
    }

    private fun cachedOrError(
        cacheKey: String,
        code: String,
        errorMessage: String,
    ): SearchUseCaseResult {
        val cached = cacheStore.get<SearchResult>(cacheKey)
        return if (cached != null) {
            println("[search] cache hit key=$cacheKey")
            SearchUseCaseResult.Cached(
                result = cached.copy(isFromCache = true),
                message = "Estás viendo resultados guardados. Actualizaremos cuando vuelva la conexión.",
                code = code,
            )
        } else {
            println("[search] cache miss key=$cacheKey")
            SearchUseCaseResult.Error(
                message = controlledMessageFor(code, errorMessage),
                code = code,
            )
        }
    }

    private fun controlledMessageFor(code: String, fallbackMessage: String): String =
        when (code) {
            "offline" -> "No tengo conexión para buscar por el momento."
            "http_error" -> "No pudimos completar la búsqueda. Puedes reintentar."
            "invalid_response" -> "La respuesta de búsqueda no tiene el formato esperado."
            else -> fallbackMessage.ifBlank { "No pudimos completar la búsqueda. Puedes reintentar." }
        }

    private fun normalizeQuery(query: String): String =
        query.trim().lowercase().replace(Regex("\\s+"), "_")
}
