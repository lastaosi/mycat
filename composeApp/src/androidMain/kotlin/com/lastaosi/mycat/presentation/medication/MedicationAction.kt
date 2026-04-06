package com.lastaosi.mycat.presentation.medication

import com.lastaosi.mycat.domain.model.Medication
import com.lastaosi.mycat.domain.model.MedicationType

sealed class MedicationAction {

    data object FabClick : MedicationAction()
    data class EditClick(val medication: Medication) : MedicationAction()
    data class DeleteClick(val medicationId: Long) : MedicationAction()
    data class ToggleActive(val medication: Medication) : MedicationAction()
    data object DialogDismiss : MedicationAction()
    data class MedicationSave(
        val name: String,
        val medicationType: MedicationType,
        val dosage: String,
        val startDate: Long,
        val endDate: Long?,
        val intervalDays: Int?,
        val memo: String,
        val alarmTimes: List<String> = emptyList()) : MedicationAction()
}