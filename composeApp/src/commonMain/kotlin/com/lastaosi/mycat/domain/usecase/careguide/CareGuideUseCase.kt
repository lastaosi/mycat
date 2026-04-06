package com.lastaosi.mycat.domain.usecase.careguide

import com.lastaosi.mycat.domain.usecase.CalculateAgeMonthUseCase
import com.lastaosi.mycat.domain.usecase.breed.GetAllBreedGuidesUseCase
import com.lastaosi.mycat.domain.usecase.cat.GetRepresentativeCatUseCase

data class CareGuideUseCase(
    val getRepresentativeCat: GetRepresentativeCatUseCase,
    val calculateAgeMonth: CalculateAgeMonthUseCase,
    val getAllBreedGuide : GetAllBreedGuidesUseCase
)