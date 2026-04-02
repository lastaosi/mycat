package com.lastaosi.mycat.domain.repository

import com.lastaosi.mycat.domain.model.CatDiary
import kotlinx.coroutines.flow.Flow

interface CatDiaryRepository {
    fun getDiariesByCat(catId: Long): Flow<List<CatDiary>>
    suspend fun getDiaryById(id: Long): CatDiary?
    suspend fun insert(diary: CatDiary): Long
    suspend fun update(diary: CatDiary)
    suspend fun delete(id: Long)
}