package com.lastaosi.mycat.domain.usecase.cat

import com.lastaosi.mycat.domain.model.Cat
import com.lastaosi.mycat.domain.repository.CatRepository
import kotlinx.coroutines.flow.Flow

class GetAllCatsUseCase(
    private val catRepository: CatRepository
) {
    operator fun invoke(): Flow<List<Cat>> = catRepository.getAllCats()
}