package com.lastaosi.mycat.domain.usecase

import com.lastaosi.mycat.domain.usecase.cat.GetAllCatsUseCase
import com.lastaosi.mycat.domain.usecase.cat.GetRepresentativeCatUseCase
import com.lastaosi.mycat.domain.usecase.cat.SetRepresentativeCatUseCase
import com.lastaosi.mycat.domain.usecase.breed.GetBreedGuideUseCase
import com.lastaosi.mycat.domain.usecase.diary.GetDiariesUseCase
import com.lastaosi.mycat.domain.usecase.healthcheck.GetHealthCheckSummaryUseCase
import com.lastaosi.mycat.domain.usecase.medication.GetMedicationsUseCase
import com.lastaosi.mycat.domain.usecase.tip.GetRandomTipUseCase
import com.lastaosi.mycat.domain.usecase.vaccination.GetUpcomingVaccinationsUseCase
import com.lastaosi.mycat.domain.usecase.weight.GetLatestWeightUseCase
import com.lastaosi.mycat.domain.usecase.weight.GetWeightHistoryUseCase

data class MainUseCase(
    val getAllCats: GetAllCatsUseCase,
    val setRepresentative: SetRepresentativeCatUseCase,
    val getBreedGuide: GetBreedGuideUseCase,
    val getLatestWeight: GetWeightHistoryUseCase,
    val getUpcomingVaccinations: GetUpcomingVaccinationsUseCase,
    val getMedications: GetMedicationsUseCase,
    val getDiaries: GetDiariesUseCase,
    val getRandomTip: GetRandomTipUseCase,
    val calculateAgeMonth: CalculateAgeMonthUseCase,
    val getHealthCheckSummary: GetHealthCheckSummaryUseCase
)