package com.lastaosi.mycat.presentation.healthcheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastaosi.mycat.domain.repository.CatRepository
import com.lastaosi.mycat.domain.repository.HealthChecklistRepository
import com.lastaosi.mycat.domain.usecase.CalculateAgeMonthUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HealthCheckViewModel(
    private val catRepository: CatRepository,
    private val healthChecklistRepository: HealthChecklistRepository,
    private val calculateAgeMonthUseCase: CalculateAgeMonthUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthCheckUiState())
    val uiState: StateFlow<HealthCheckUiState> = _uiState

    // HealthCheckViewModel - loadData 대신 init에서 대표 고양이 로드
    init {
        viewModelScope.launch {
            val cat = catRepository.getRepresentativeCat() ?: return@launch
            val ageMonth = calculateAgeMonthUseCase(cat.birthDate)

            android.util.Log.d("HealthCheck", "birthDate: ${cat.birthDate}")
            android.util.Log.d("HealthCheck", "ageMonth: $ageMonth")
            _uiState.update { it.copy(catName = cat.name, ageMonth = ageMonth, isLoading = true) }

            healthChecklistRepository.getAllChecklist()
                .collect { items ->
                    _uiState.update { it.copy(allItems = items, isLoading = false) }
                }
        }
    }

    fun loadData(catId: Long) {
        viewModelScope.launch {
            val cat = catRepository.getCatById(catId) ?: return@launch
            val ageMonth = calculateAgeMonthUseCase(cat.birthDate)
            _uiState.update { it.copy(catName = cat.name, ageMonth = ageMonth, isLoading = true) }

            // 현재 개월수까지의 전체 체크리스트
            healthChecklistRepository.getChecklistUpToMonth(ageMonth)
                .collect { items ->
                    _uiState.update {
                        it.copy(allItems = items, isLoading = false)
                    }
                }
        }
    }

    fun onTabSelected(tab: HealthCheckTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
}