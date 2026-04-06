package com.lastaosi.mycat.presentation.healthcheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastaosi.mycat.domain.repository.CatRepository
import com.lastaosi.mycat.domain.repository.HealthChecklistRepository
import com.lastaosi.mycat.domain.usecase.CalculateAgeMonthUseCase
import com.lastaosi.mycat.domain.usecase.healthcheck.HealthCheckUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HealthCheckViewModel(
    private val useCase: HealthCheckUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthCheckUiState())
    val uiState: StateFlow<HealthCheckUiState> = _uiState

    // HealthCheckViewModel - loadData 대신 init에서 대표 고양이 로드
    init {
        viewModelScope.launch {
            val cat = useCase.getRepresentativeCat() ?: return@launch
            val ageMonth = useCase.calculateAgeMonth(cat.birthDate)

            _uiState.update { it.copy(catName = cat.name, ageMonth = ageMonth, isLoading = true) }

            useCase.getHealthCheckList()
                .collect { items ->
                    _uiState.update { it.copy(allItems = items, isLoading = false) }
                }
        }
    }


    fun onTabSelected(tab: HealthCheckTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
}