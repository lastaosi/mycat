package com.lastaosi.mycat.domain.usecase.cat

import com.lastaosi.mycat.domain.repository.CatRepository

class SetRepresentativeCatUseCase(
    private val catRepository: CatRepository
) {
    suspend operator fun invoke(catId: Long) = catRepository.setRepresentative(catId)
}