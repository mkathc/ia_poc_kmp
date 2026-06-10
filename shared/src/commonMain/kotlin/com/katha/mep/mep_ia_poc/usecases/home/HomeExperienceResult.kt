package com.katha.mep.mep_ia_poc.usecases.home

import com.katha.mep.mep_ia_poc.models.home.HomeExperience

sealed interface HomeExperienceResult {
    data class Success(val experience: HomeExperience) : HomeExperienceResult
    data class Cached(
        val experience: HomeExperience,
        val message: String,
        val code: String,
    ) : HomeExperienceResult

    data class Error(
        val message: String,
        val code: String,
    ) : HomeExperienceResult
}
