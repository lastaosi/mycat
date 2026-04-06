package com.lastaosi.mycat.domain.usecase.healthcheck

import com.lastaosi.mycat.domain.usecase.CalculateAgeMonthUseCase
import com.lastaosi.mycat.domain.usecase.cat.GetCatByIdUseCase
import com.lastaosi.mycat.domain.usecase.cat.GetRepresentativeCatUseCase

data class HealthCheckUseCase(
    val getRepresentativeCat: GetRepresentativeCatUseCase,  // 추가
    val calculateAgeMonth: CalculateAgeMonthUseCase,
    val getHealthCheckList: GetHealthChecklistUseCase
)

