package com.lastaosi.mycat.domain.usecase.diary

import com.lastaosi.mycat.domain.usecase.cat.GetCatByIdUseCase

data class DiaryUseCase(
    val getCatById: GetCatByIdUseCase,
    val getDiaries: GetDiariesUseCase,
    val saveDiary: SaveDiaryUseCase,
    val deleteDiary: DeleteDiaryUseCase
)