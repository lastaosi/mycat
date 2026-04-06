package com.lastaosi.mycat.domain.usecase.weight

import com.lastaosi.mycat.domain.usecase.breed.GetBreedAverageDataUseCase
import com.lastaosi.mycat.domain.usecase.cat.GetCatByIdUseCase

data class WeightUseCase(
    val getCatById: GetCatByIdUseCase,
    val getWeightHistory: GetWeightHistoryUseCase,
    val insertWeight: InsertWeightUseCase,
    val getBreedAverageData: GetBreedAverageDataUseCase
)