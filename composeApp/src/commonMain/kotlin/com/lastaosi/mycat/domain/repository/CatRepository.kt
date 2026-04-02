package com.lastaosi.mycat.domain.repository

import com.lastaosi.mycat.domain.model.Cat
import kotlinx.coroutines.flow.Flow

interface CatRepository {
    fun getAllCats(): Flow<List<Cat>>
    suspend fun getCatById(id: Long): Cat?
    suspend fun getRepresentativeCat(): Cat?
    suspend fun insert(cat: Cat)        // Long 제거
    suspend fun update(cat: Cat)
    suspend fun delete(cat: Cat)
    suspend fun setRepresentative(id: Long)
    suspend fun getCount(): Long
}