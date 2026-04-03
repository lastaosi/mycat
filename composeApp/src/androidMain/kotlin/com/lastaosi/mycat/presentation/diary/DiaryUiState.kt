package com.lastaosi.mycat.presentation.diary

import com.lastaosi.mycat.domain.model.CatDiary
import com.lastaosi.mycat.domain.model.DiaryMood

data class DiaryUiState(
    val catId: Long = 0L,
    val catName: String = "",
    val diaries: List<CatDiary> = emptyList(),
    val showInputDialog: Boolean = false,
    val editingDiary: CatDiary? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)