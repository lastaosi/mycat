package com.lastaosi.mycat.presentation.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastaosi.mycat.domain.model.BreedAvgPoint
import com.lastaosi.mycat.domain.model.WeightRecord
import com.lastaosi.mycat.domain.repository.BreedRepository
import com.lastaosi.mycat.domain.repository.CatRepository
import com.lastaosi.mycat.domain.repository.WeightRecordRepository
import com.lastaosi.mycat.domain.usecase.breed.GetBreedAverageDataUseCase
import com.lastaosi.mycat.domain.usecase.cat.GetCatByIdUseCase
import com.lastaosi.mycat.domain.usecase.weight.GetWeightHistoryUseCase
import com.lastaosi.mycat.domain.usecase.weight.InsertWeightUseCase
import com.lastaosi.mycat.domain.usecase.weight.WeightUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 체중 기록 화면 ViewModel.
 *
 * [loadData]로 고양이 ID를 받아 체중 히스토리와 품종 평균 성장 데이터를 로드한다.
 * FAB 클릭 시 입력 다이얼로그를 표시하고, 저장 시 WeightRecord를 DB에 삽입한다.
 */
class WeightViewModel(
    private val useCase: WeightUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeightUiState())
    val uiState: StateFlow<WeightUiState> = _uiState

    fun loadData(catId: Long) {
        viewModelScope.launch {
            val cat = useCase.getCatById(catId) ?: return@launch
            _uiState.update {
                it.copy(catId = cat.id, catName = cat.name, birthDate = cat.birthDate)
            }

            // 체중 기록 Flow 구독
            launch {
                useCase.getWeightHistory(cat.id)
                    .collect { records ->
                        val sorted = records.sortedBy { it.recordedAt }
                        _uiState.update {
                            it.copy(
                                weightHistory = sorted,
                                latestWeightG = sorted.lastOrNull()?.weightG
                            )
                        }
                    }
            }

            // 품종 평균 성장 데이터
            cat.breedId?.let { breedId ->
                launch {
                    val avgPoints = useCase.getBreedAverageData(breedId)
                    _uiState.update { it.copy(breedAverageData = avgPoints) }
                }
            }
        }
    }

    fun onAction(action: WeightAction) {
        when (action) {
            is WeightAction.TabSelected -> onTabSelected(action.tab)
            is WeightAction.FabClick -> onFabClick()
            is WeightAction.DialogDismiss -> onDialogDismiss()
            is WeightAction.WeightSave -> onWeightSave(action.weightKg, action.memo)
        }
    }

    private fun onTabSelected(tab: WeightTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun onFabClick() {
        _uiState.update { it.copy(showInputDialog = true) }
    }

    private fun onDialogDismiss() {
        _uiState.update { it.copy(showInputDialog = false) }
    }

    @OptIn(ExperimentalTime::class)
    private fun onWeightSave(weightKg: String, memo: String) {
        val weightG = weightKg.toDoubleOrNull()?.times(1000)?.toInt() ?: return
        viewModelScope.launch {
           useCase.insertWeight(
                WeightRecord(
                    catId = _uiState.value.catId,
                    weightG = weightG,
                    recordedAt = Clock.System.now().toEpochMilliseconds(),
                    memo = memo.ifBlank { null }
                )
            )
            _uiState.update { it.copy(showInputDialog = false) }
        }
    }
}