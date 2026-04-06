package com.lastaosi.mycat.domain.usecase.breed

import com.lastaosi.mycat.domain.model.BreedAvgPoint
import com.lastaosi.mycat.domain.repository.BreedRepository

class GetBreedAverageDataUseCase(
    private val breedRepository: BreedRepository
) {
    suspend operator fun invoke(breedId: Int): List<BreedAvgPoint> =
        breedRepository.getAllGuidesByBreed(breedId)
            .map { guide ->
                BreedAvgPoint(
                    month = guide.month,
                    weightMinG = guide.weightMinG,
                    weightMaxG = guide.weightMaxG,
                    avgWeightG = (guide.weightMinG + guide.weightMaxG) / 2
                )
            }
            .sortedBy { it.month }
}