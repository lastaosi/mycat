package com.lastaosi.mycat.domain.usecase

import com.lastaosi.mycat.domain.model.Cat
import com.lastaosi.mycat.domain.repository.CatRepository

/**
 * 고양이 등록 UseCase.
 * Cat 도메인 모델을 받아 DB에 저장한다.
 */
class InsertCatUseCase(
    private val catRepository: CatRepository
) {
    suspend operator fun invoke(cat: Cat) {
        catRepository.insert(cat)
    }
}