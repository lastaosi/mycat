package com.lastaosi.mycat.presentation.vaccination

import com.lastaosi.mycat.domain.model.VaccinationRecord

sealed class VaccinationAction {
    data object FabClick : VaccinationAction()
    data object DialogDismiss : VaccinationAction()
    data class EditClick(val record: VaccinationRecord) : VaccinationAction()
    data class DeleteClick(val recordId: Long) : VaccinationAction()
    data class VaccinationSave(val title: String,
                               val vaccinatedAt: Long,
                               val nextDueAt: Long?,
                               val memo: String,
                               val isNotificationEnabled: Boolean) : VaccinationAction()
}
