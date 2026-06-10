package com.katha.mep.mep_ia_poc.usecases.emergency

import com.katha.mep.mep_ia_poc.contracts.EmergencySupportService
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyReport

class GetPendingEmergencyReportsUseCase(
    private val emergencySupportService: EmergencySupportService,
) {
    suspend operator fun invoke(): List<EmergencyReport> =
        emergencySupportService.getPendingReports()
}
