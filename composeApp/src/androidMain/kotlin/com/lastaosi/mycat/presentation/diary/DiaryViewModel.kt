package com.lastaosi.mycat.presentation.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastaosi.mycat.domain.model.CatDiary
import com.lastaosi.mycat.domain.model.DiaryMood
import com.lastaosi.mycat.domain.repository.CatDiaryRepository
import com.lastaosi.mycat.domain.repository.CatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DiaryViewModel(
    private val catRepository: CatRepository,
    private val catDiaryRepository: CatDiaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState

    fun loadData(catId: Long) {
        viewModelScope.launch {
            val cat = catRepository.getCatById(catId) ?: return@launch
            _uiState.update { it.copy(catId = cat.id, catName = cat.name) }

            catDiaryRepository.getDiariesByCat(catId)
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
                catDiaryRepository.update(
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
                catDiaryRepository.insert(
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
            catDiaryRepository.delete(id)
        }
    }
}