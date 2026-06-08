package com.katha.mep.mep_ia_poc.contracts

import com.katha.mep.mep_ia_poc.models.emergency.EmergencyAnswer
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyContact
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyGuide
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyReport
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyType

interface EmergencySupportService {
    suspend fun getOfflineGuide(type: EmergencyType): EmergencyGuide
    suspend fun getEmergencyContacts(): List<EmergencyContact>
    suspend fun createLocalReport(type: EmergencyType, answers: List<EmergencyAnswer>): EmergencyReport
    suspend fun getPendingReports(): List<EmergencyReport>
    suspend fun syncPendingActions()
}
