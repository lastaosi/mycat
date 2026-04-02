package com.lastaosi.mycat.presentation.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastaosi.mycat.domain.model.WeightRecord
import com.lastaosi.mycat.domain.repository.BreedRepository
import com.lastaosi.mycat.domain.repository.CatRepository
import com.lastaosi.mycat.domain.repository.WeightRecordRepository
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
    private val catRepository: CatRepository,
    private val weightRecordRepository: WeightRecordRepository,
    private val breedRepository: BreedRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeightUiState())
    val uiState: StateFlow<WeightUiState> = _uiState

    // init 제거 — LaunchedEffect에서 catId 받아서 호출
    fun loadData(catId: Long) {
        viewModelScope.launch {
            val cat = catRepository.getCatById(catId) ?: return@launch
            _uiState.update { it.copy(
                catId = cat.id, catName = cat.name, birthDate = cat.birthDate  // 추가
            ) }

            // 체중 기록 Flow 구독
            launch {
                weightRecordRepository.getWeightHistory(cat.id)
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
                    val guides = breedRepository.getAllGuidesByBreed(breedId)
                    val avgPoints = guides
                        .map { guide ->
                            BreedAvgPoint(
                                month = guide.month,
                                weightMinG = guide.weightMinG,
                                weightMaxG = guide.weightMaxG,
                                avgWeightG = (guide.weightMinG + guide.weightMaxG) / 2
                            )
                        }
                        .sortedBy { it.month }
                    _uiState.update { it.copy(breedAverageData = avgPoints) }
                }
            }
        }
    }

    fun onTabSelected(tab: WeightTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onFabClick() {
        _uiState.update { it.copy(showInputDialog = true) }
    }

    fun onDialogDismiss() {
        _uiState.update { it.copy(showInputDialog = false) }
    }

    @OptIn(ExperimentalTime::class)
    fun onWeightSave(weightKg: String, memo: String) {
        val weightG = weightKg.toDoubleOrNull()?.times(1000)?.toInt() ?: return
        viewModelScope.launch {
            weightRecordRepository.insert(
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