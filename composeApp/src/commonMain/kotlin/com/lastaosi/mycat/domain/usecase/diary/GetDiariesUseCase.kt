package com.lastaosi.mycat.domain.usecase.diary

import com.lastaosi.mycat.domain.model.CatDiary
import com.lastaosi.mycat.domain.repository.CatDiaryRepository
import kotlinx.coroutines.flow.Flow

class GetDiariesUseCase(
    private val catDiaryRepository: CatDiaryRepository
) {
    operator fun invoke(catId: Long): Flow<List<CatDiary>> =
        catDiaryRepository.getDiariesByCat(catId)
}