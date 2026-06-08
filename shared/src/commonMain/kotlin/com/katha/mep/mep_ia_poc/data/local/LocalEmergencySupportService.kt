package com.katha.mep.mep_ia_poc.data.local

import com.katha.mep.mep_ia_poc.cache.LocalCacheStore
import com.katha.mep.mep_ia_poc.contracts.EmergencySupportService
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyAnswer
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyContact
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyGuide
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyReport
import com.katha.mep.mep_ia_poc.models.emergency.EmergencySyncStatus
import com.katha.mep.mep_ia_poc.models.emergency.EmergencyType

class LocalEmergencySupportService(
    private val cacheStore: LocalCacheStore,
) : EmergencySupportService {
    override suspend fun getOfflineGuide(type: EmergencyType): EmergencyGuide =
        EmergencyGuide(
            id = "guide-${type.name}",
            type = type,
            title = type.displayName,
            steps = listOf("Mantente en una zona segura.", "Contacta asistencia.", "Registra el reporte cuando puedas."),
            contacts = getEmergencyContacts(),
        )

    override suspend fun getEmergencyContacts(): List<EmergencyContact> = listOf(
        EmergencyContact("central", "Central de emergencias", "000000000", "24/7"),
        EmergencyContact("mep", "Asistencia MEP", "111111111", "24/7"),
    )

    override suspend fun createLocalReport(
        type: EmergencyType,
        answers: List<EmergencyAnswer>,
    ): EmergencyReport {
        val current = getAllReports()
        val report = EmergencyReport(
            id = "emergency-report-${current.size + 1}",
            type = type,
            answers = answers,
            createdAt = "local-now",
        )
        cacheStore.save(LocalCacheStore.EmergencyReportsKey, current + report)
        return report
    }

    override suspend fun getPendingReports(): List<EmergencyReport> =
        getAllReports().filter { it.status == EmergencySyncStatus.pendingSync }

    override suspend fun syncPendingActions() {
        val synced = getAllReports().map { it.copy(status = EmergencySyncStatus.synced) }
        cacheStore.save(LocalCacheStore.EmergencyReportsKey, synced)
    }

    private fun getAllReports(): List<EmergencyReport> =
        cacheStore.get<List<EmergencyReport>>(LocalCacheStore.EmergencyReportsKey).orEmpty()
}
