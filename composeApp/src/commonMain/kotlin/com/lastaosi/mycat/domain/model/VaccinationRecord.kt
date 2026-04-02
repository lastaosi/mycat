package com.lastaosi.mycat.domain.model

data class VaccinationRecord(
    val id: Long = 0,
    val catId: Long,
    val checklistId: Int? = null,
    val title: String,
    val vaccinatedAt: Long,
    val nextDueAt: Long? = null,
    val memo: String? = null,
    val isNotificationEnabled: Boolean = true
)