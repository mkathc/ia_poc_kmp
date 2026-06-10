package com.katha.mep.mep_ia_poc.models.emergency

enum class EmergencyType(val displayName: String) {
    carAccident("Accidente vehicular"),
    medicalEmergency("Emergencia médica"),
    theftAssistance("Robo / asistencia"),
}

data class EmergencyGuide(
    val id: String,
    val type: EmergencyType,
    val title: String,
    val steps: List<String>,
    val contacts: List<EmergencyContact>,
)

data class EmergencyContact(
    val id: String,
    val label: String,
    val phone: String,
    val available: String,
)

data class EmergencyConversationStep(
    val id: String,
    val question: String,
    val options: List<String>,
    val order: Int,
)

data class EmergencyAnswer(
    val questionId: String,
    val question: String,
    val answer: String,
)

data class EmergencyReport(
    val id: String,
    val type: EmergencyType,
    val answers: List<EmergencyAnswer>,
    val createdAt: String,
    val status: EmergencySyncStatus = EmergencySyncStatus.pendingSync,
)

enum class EmergencySyncStatus {
    pendingSync,
    synced,
}
