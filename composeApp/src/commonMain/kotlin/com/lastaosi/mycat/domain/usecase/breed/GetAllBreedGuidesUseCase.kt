package com.lastaosi.mycat.domain.usecase.breed

import com.lastaosi.mycat.domain.model.BreedMonthlyGuide
import com.lastaosi.mycat.domain.repository.BreedRepository

class GetAllBreedGuidesUseCase(
    private val breedRepository: BreedRepository
) {
    suspend operator fun invoke(breedId: Int): List<BreedMonthlyGuide> =
        breedRepository.getAllGuidesByBreed(breedId )
}