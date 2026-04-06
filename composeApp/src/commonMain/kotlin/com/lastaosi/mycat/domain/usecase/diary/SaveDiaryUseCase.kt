package com.lastaosi.mycat.domain.usecase.diary

import com.lastaosi.mycat.domain.model.CatDiary
import com.lastaosi.mycat.domain.repository.CatDiaryRepository

class SaveDiaryUseCase(
    private val catDiaryRepository: CatDiaryRepository
) {
    suspend operator fun invoke(diary: CatDiary): Long {
        return if (diary.id == 0L) {
            catDiaryRepository.insert(diary)
        } else {
            catDiaryRepository.update(diary)
            diary.id
        }
    }
}