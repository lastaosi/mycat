package com.lastaosi.mycat.presentation.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastaosi.mycat.domain.model.CatDiary
import com.lastaosi.mycat.domain.model.DiaryMood
import com.lastaosi.mycat.domain.usecase.diary.DiaryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DiaryViewModel(
    private val useCase: DiaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState

    fun loadData(catId: Long) {
        viewModelScope.launch {
            val cat = useCase.getCatById(catId) ?: return@launch
            _uiState.update { it.copy(catId = cat.id, catName = cat.name) }

            useCase.getDiaries(catId)
                .collect { diaries ->
                    _uiState.update {
                        it.copy(diaries = diaries.sortedByDescending { d -> d.createdAt })
                    }
                }
        }
    }

    fun onFabClick() {
        _uiState.update { it.copy(showInputDialog = true, editingDiary = null) }
    }

    fun onEditClick(diary: CatDiary) {
        _uiState.update { it.copy(showInputDialog = true, editingDiary = diary) }
    }

    fun onDialogDismiss() {
        _uiState.update { it.copy(showInputDialog = false, editingDiary = null) }
    }

    fun onAction(action: DiaryAction){
        when(action){
            is DiaryAction.FabClick -> onFabClick()
            is DiaryAction.EditClick -> onEditClick(action.diary)
            is DiaryAction.DialogDismiss -> onDialogDismiss()
            is DiaryAction.DeleteClick -> onDelete(action.diaryId)
            is DiaryAction.DiarySave -> onSave(action.title,action.content,action.mood,action.photoPath,action.dateMillis)
        }
    }

    @OptIn(ExperimentalTime::class)
    fun onSave(
        title: String,
        content: String,
        mood: DiaryMood?,
        photoPath: String?,
        dateMillis: Long
    ) {
        viewModelScope.launch {
            val editing = _uiState.value.editingDiary
            val now = Clock.System.now().toEpochMilliseconds()
            if (editing != null) {
                useCase.saveDiary(
                    editing.copy(
                        title = title.ifBlank { null },
                        content = content,
                        mood = mood,
                        photoPath = photoPath,
                        createdAt = dateMillis,
                        updatedAt = now
                    )
                )
            } else {
                useCase.saveDiary(
                    CatDiary(
                        catId = _uiState.value.catId,
                        title = title.ifBlank { null },
                        content = content,
                        mood = mood,
                        photoPath = photoPath,
                        createdAt = dateMillis,
                        updatedAt = now
                    )
                )
            }
            _uiState.update { it.copy(showInputDialog = false, editingDiary = null) }
        }
    }

    fun onDelete(id: Long) {
        viewModelScope.launch {
            useCase.deleteDiary(id)
        }
    }
}