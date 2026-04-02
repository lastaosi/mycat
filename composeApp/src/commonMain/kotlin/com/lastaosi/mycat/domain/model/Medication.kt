package com.lastaosi.mycat.domain.model

data class Medication(
    val id: Long = 0,
    val catId: Long,
    val name: String,
    val dosage: String? = null,
    val medicationType: MedicationType,
    val intervalDays: Int? = null,
    val startDate: Long,
    val endDate: Long? = null,
    val memo: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = 0
)

enum class MedicationType {
    ONCE, DAILY, INTERVAL, PERIOD
}

data class MedicationAlarm(
    val id: Long = 0,
    val medicationId: Long,
    val alarmTime: String,  // "08:00"
    val isEnabled: Boolean = true
)

data class MedicationLog(
    val id: Long = 0,
    val medicationId: Long,
    val scheduledAt: Long,
    val takenAt: Long? = null,
    val isSkipped: Boolean = false
)