package com.lastaosi.mycat.presentation.careguide

import com.lastaosi.mycat.domain.model.BreedMonthlyGuide

data class CareGuideUiState(
    val catName:String ="",
    val breedName: String = "",
    val ageMonth: Int = 0,
    val guides : List<BreedMonthlyGuide> = emptyList(),
    val hasBreed : Boolean = true,
    val isLoading : Boolean = false
)