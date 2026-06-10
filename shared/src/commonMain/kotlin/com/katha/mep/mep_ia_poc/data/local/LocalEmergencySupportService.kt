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
            title = type.guideTitle(),
            steps = type.guideSteps(),
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

private fun EmergencyType.guideTitle(): String =
    when (this) {
        EmergencyType.carAccident -> "Qué hacer ante un accidente vehicular"
        EmergencyType.medicalEmergency -> "Qué hacer ante una emergencia médica"
        EmergencyType.theftAssistance -> "Qué hacer ante robo o asistencia"
    }

private fun EmergencyType.guideSteps(): List<String> =
    when (this) {
        EmergencyType.carAccident -> listOf(
            "Mantén la calma y verifica si hay heridos.",
            "Colócate en una zona segura.",
            "Comunícate con la central de emergencias.",
            "Toma fotos si es seguro hacerlo.",
            "Registra datos del incidente.",
        )
        EmergencyType.medicalEmergency -> listOf(
            "Verifica el estado de la persona afectada.",
            "Comunícate con la central de emergencias.",
            "Ten a la mano tu documento y datos de póliza.",
            "Sigue las indicaciones del operador.",
        )
        EmergencyType.theftAssistance -> listOf(
            "Ponte en una zona segura.",
            "Comunícate con la central de asistencia.",
            "Registra lo ocurrido.",
            "Evita exponerte a riesgos adicionales.",
        )
    }
