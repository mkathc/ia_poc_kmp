package com.katha.mep.mep_ia_poc.usecases.home

import com.katha.mep.mep_ia_poc.cache.LocalCacheStore
import com.katha.mep.mep_ia_poc.config.MockUserProfile
import com.katha.mep.mep_ia_poc.config.PocConfig
import com.katha.mep.mep_ia_poc.contracts.PersonalizationService
import com.katha.mep.mep_ia_poc.contracts.PersonalizationServiceException
import com.katha.mep.mep_ia_poc.models.chat.ConversationContext
import com.katha.mep.mep_ia_poc.models.home.HomeExperience

class GetHomeExperienceUseCase(
    private val personalizationServiceProvider: () -> PersonalizationService,
    private val cacheStore: LocalCacheStore,
    private val configProvider: () -> PocConfig,
) {
    suspend operator fun invoke(
        context: ConversationContext,
        profile: MockUserProfile,
    ): HomeExperienceResult {
        val config = configProvider()
        val cacheKey = LocalCacheStore.homeKey(config.provider.id, profile.profileName)

        return try {
            val experience = personalizationServiceProvider().getHomeExperience(context, profile)
            println("[home] cache save key=$cacheKey")
            cacheStore.save(cacheKey, experience)
            HomeExperienceResult.Success(experience)
        } catch (exception: PersonalizationServiceException) {
            cachedOrError(cacheKey, exception.code, exception.message.orEmpty())
        } catch (exception: Throwable) {
            cachedOrError(cacheKey, "unknown_error", exception.message.orEmpty())
        }
    }

    private fun cachedOrError(
        cacheKey: String,
        code: String,
        errorMessage: String,
    ): HomeExperienceResult {
        println("[home] cache lookup key=$cacheKey")
        val cached = cacheStore.get<HomeExperience>(cacheKey)
        return if (cached != null) {
            println("[home] cache hit key=$cacheKey")
            HomeExperienceResult.Cached(
                experience = cached.copy(isFromCache = true),
                message = "Estás viendo información guardada. Actualizaremos cuando vuelva la conexión.",
                code = code,
            )
        } else {
            println("[home] cache miss key=$cacheKey")
            HomeExperienceResult.Error(
                message = controlledMessageFor(code, errorMessage),
                code = code,
            )
        }
    }

    private fun controlledMessageFor(code: String, fallbackMessage: String): String =
        when (code) {
            "offline" -> "No tengo conexión para actualizar Home por el momento."
            "http_error" -> "No pudimos actualizar Home. Puedes reintentar."
            "invalid_response" -> "La respuesta de Home no tiene el formato esperado."
            else -> fallbackMessage.ifBlank { "No pudimos actualizar Home. Puedes reintentar." }
        }
}
