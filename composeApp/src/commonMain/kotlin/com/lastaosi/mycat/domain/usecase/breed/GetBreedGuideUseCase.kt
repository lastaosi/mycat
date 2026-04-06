package com.lastaosi.mycat.domain.usecase.breed

import com.lastaosi.mycat.domain.model.BreedMonthlyGuide
import com.lastaosi.mycat.domain.repository.BreedRepository

class GetBreedGuideUseCase(
    private val breedRepository: BreedRepository
) {
    suspend operator fun invoke(breedId: Int, ageMonth: Int): BreedMonthlyGuide? =
        breedRepository.getGuideForMonth(breedId, ageMonth)
}