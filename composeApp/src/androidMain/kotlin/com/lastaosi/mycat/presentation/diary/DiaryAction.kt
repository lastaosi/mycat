package com.lastaosi.mycat.presentation.diary

import com.lastaosi.mycat.domain.model.CatDiary
import com.lastaosi.mycat.domain.model.DiaryMood

sealed class DiaryAction {
    data object FabClick : DiaryAction()
    data class EditClick(val diary: CatDiary) : DiaryAction()
    data class DeleteClick(val diaryId: Long) : DiaryAction()
    data class DialogDismiss(val diaryId: Long) : DiaryAction()
    data class DiarySave(
        val title: String,
        val content: String,
        val mood: DiaryMood?,
        val photoPath: String?,
        val dateMillis: Long
    ) : DiaryAction()
}