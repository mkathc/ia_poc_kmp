package com.katha.mep.mep_ia_poc.usecases.emergency

import com.katha.mep.mep_ia_poc.contracts.EmergencySupportService
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyAnswer
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyReport
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyType

class CreateEmergencyReportUseCase(
    private val emergencySupportService: EmergencySupportService,
) {
    suspend operator fun invoke(type: EmergencyType, answers: List<EmergencyAnswer>): EmergencyReport =
        emergencySupportService.createLocalReport(type, answers)
}
