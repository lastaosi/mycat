package com.lastaosi.mycat.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lastaosi.mycat.db.MyCatDatabase
import com.lastaosi.mycat.domain.model.CatDiary
import com.lastaosi.mycat.domain.model.DiaryMood
import com.lastaosi.mycat.domain.repository.CatDiaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CatDiaryRepositoryImpl(
    private val db: MyCatDatabase
) : CatDiaryRepository {

    override fun getDiariesByCat(catId: Long): Flow<List<CatDiary>> =
        db.catDiaryQueries.getDiariesByCat(catId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getDiaryById(id: Long): CatDiary? =
        withContext(Dispatchers.IO) {
            db.catDiaryQueries.getDiaryById(id).executeAsOneOrNull()?.toDomain()
        }

    override suspend fun insert(diary: CatDiary): Long =
        withContext(Dispatchers.IO) {
            db.catDiaryQueries.insert(
                catId = diary.catId,
                title = diary.title,
                content = diary.content,
                mood = diary.mood?.name,
                photoPath = diary.photoPath,
                createdAt = diary.createdAt,
                updatedAt = diary.updatedAt
            )
            db.catDiaryQueries.lastInsertRowId().executeAsOne()
        }

    override suspend fun update(diary: CatDiary) =
        withContext(Dispatchers.IO) {
            db.catDiaryQueries.update(
                id = diary.id,
                title = diary.title,
                content = diary.content,
                mood = diary.mood?.name,
                photoPath = diary.photoPath,
                updatedAt = diary.updatedAt
            )
            Unit
        }

    override suspend fun delete(id: Long) =
        withContext(Dispatchers.IO) {
            db.catDiaryQueries.delete(id)
            Unit
        }

    private fun com.lastaosi.mycat.db.Cat_diary.toDomain() = CatDiary(
        id = id,
        catId = catId,
        title = title,
        content = content,
        mood = mood?.let { runCatching { DiaryMood.valueOf(it) }.getOrNull() },
        photoPath = photoPath,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
