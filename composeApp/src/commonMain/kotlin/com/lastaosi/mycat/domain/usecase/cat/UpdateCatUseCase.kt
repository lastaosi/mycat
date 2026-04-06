package com.lastaosi.mycat.domain.usecase.cat

import com.lastaosi.mycat.domain.model.Cat
import com.lastaosi.mycat.domain.repository.CatRepository

class UpdateCatUseCase(
    private val catRepository: CatRepository
) {
    suspend operator fun invoke(cat: Cat) = catRepository.update(cat)
}