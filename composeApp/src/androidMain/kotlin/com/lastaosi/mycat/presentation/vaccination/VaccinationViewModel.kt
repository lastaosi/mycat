package com.lastaosi.mycat.presentation.vaccination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastaosi.mycat.domain.model.VaccinationRecord
import com.lastaosi.mycat.domain.repository.CatRepository
import com.lastaosi.mycat.domain.repository.VaccinationRecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

class VaccinationViewModel(
    private val catRepository: CatRepository,
    private val vaccinationRecordRepository: VaccinationRecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaccinationUiState())
    val uiState: StateFlow<VaccinationUiState> = _uiState

    fun loadData(catId: Long) {
        viewModelScope.launch {
            val cat = catRepository.getCatById(catId) ?: return@launch
            _uiState.update { it.copy(catId = cat.id, catName = cat.name) }

            vaccinationRecordRepository.getVaccinationsByCat(catId)
                .collect { records ->
                    _uiState.update {
                        it.copy(
                            records = records.sortedByDescending { r -> r.vaccinatedAt }
                        )
                    }
                }
        }
    }

    fun onFabClick() {
        _uiState.update { it.copy(showInputDialog = true, editingRecord = null) }
    }

    fun onEditClick(record: VaccinationRecord) {
        _uiState.update { it.copy(showInputDialog = true, editingRecord = record) }
    }

    fun onDialogDismiss() {
        _uiState.update { it.copy(showInputDialog = false, editingRecord = null) }
    }

    fun onSave(
        title: String,
        vaccinatedAt: Long,
        nextDueAt: Long?,
        memo: String,
        isNotificationEnabled: Boolean
    ) {
        viewModelScope.launch {
            val editing = _uiState.value.editingRecord
            if (editing != null) {
                // 수정
                vaccinationRecordRepository.update(
                    editing.copy(
                        title = title,
                        vaccinatedAt = vaccinatedAt,
                        nextDueAt = nextDueAt,
                        memo = memo.ifBlank { null },
                        isNotificationEnabled = isNotificationEnabled
                    )
                )
            } else {
                // 신규
                vaccinationRecordRepository.insert(
                    VaccinationRecord(
                        catId = _uiState.value.catId,
                        title = title,
                        vaccinatedAt = vaccinatedAt,
                        nextDueAt = nextDueAt,
                        memo = memo.ifBlank { null },
                        isNotificationEnabled = isNotificationEnabled
                    )
                )
            }
            _uiState.update { it.copy(showInputDialog = false, editingRecord = null) }
        }
    }

    fun onDelete(id: Long) {
        viewModelScope.launch {
            vaccinationRecordRepository.delete(id)
        }
    }
}