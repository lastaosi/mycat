package com.lastaosi.mycat.domain.usecase.diary

import com.lastaosi.mycat.domain.repository.CatDiaryRepository

class DeleteDiaryUseCase(
    private val catDiaryRepository: CatDiaryRepository
) {
    suspend operator fun invoke(id: Long) = catDiaryRepository.delete(id)
}