package com.lastaosi.mycat.presentation.careguide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastaosi.mycat.domain.model.BreedMonthlyGuide
import com.lastaosi.mycat.domain.usecase.careguide.CareGuideUseCase
import com.lastaosi.mycat.domain.usecase.healthcheck.HealthCheckUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CareGuideViewModel(
    private val useCase: CareGuideUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CareGuideUiState())
    val uiState: StateFlow<CareGuideUiState> = _uiState

    init {
        viewModelScope.launch {
            val cat = useCase.getRepresentativeCat() ?: return@launch
            if(cat.breedId == null){
                _uiState.update { it.copy(hasBreed = false, isLoading = false) }
                return@launch
            }else{
                val ageMonth = useCase.calculateAgeMonth(cat.birthDate)
                val items = useCase.getAllBreedGuide(cat.breedId)

                _uiState.update {
                    it.copy(  catName = cat.name,
                        breedName = cat.breedNameCustom ?: "",
                        ageMonth = ageMonth,
                        guides = items,
                        isLoading = false )
                }
            }
        }

    }
}