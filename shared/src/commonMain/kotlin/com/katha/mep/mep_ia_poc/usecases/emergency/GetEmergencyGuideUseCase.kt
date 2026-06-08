package com.katha.mep.mep_ia_poc.usecases.emergency

import com.katha.mep.mep_ia_poc.contracts.EmergencySupportService
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyGuide
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyType

class GetEmergencyGuideUseCase(
    private val emergencySupportService: EmergencySupportService,
) {
    suspend operator fun invoke(type: EmergencyType): EmergencyGuide =
        emergencySupportService.getOfflineGuide(type)
}
