package com.lastaosi.mycat.presentation.vaccination

import com.lastaosi.mycat.domain.model.VaccinationRecord

data class VaccinationUiState(
    val catId: Long = 0L,
    val catName: String = "",
    val records: List<VaccinationRecord> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showInputDialog: Boolean = false,
    val editingRecord: VaccinationRecord? = null  // null이면 신규, 아니면 수정
)