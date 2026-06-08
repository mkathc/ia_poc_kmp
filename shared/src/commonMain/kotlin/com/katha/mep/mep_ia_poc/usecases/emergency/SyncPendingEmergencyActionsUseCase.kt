package com.katha.mep.mep_ia_poc.usecases.emergency

import com.katha.mep.mep_ia_poc.contracts.EmergencySupportService

class SyncPendingEmergencyActionsUseCase(
    private val emergencySupportService: EmergencySupportService,
) {
    suspend operator fun invoke() {
        emergencySupportService.syncPendingActions()
    }
}
