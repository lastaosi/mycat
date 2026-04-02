package com.lastaosi.mycat.domain.repository

import com.lastaosi.mycat.domain.model.CatTip
import com.lastaosi.mycat.domain.model.CatTipCategory
import kotlinx.coroutines.flow.Flow

interface CatTipRepository {
    fun getAllTips(): Flow<List<CatTip>>
    suspend fun getRandomTip(): CatTip?
    suspend fun getRandomTipByCategory(category: CatTipCategory): CatTip?
    fun getTipsByCategory(category: CatTipCategory): Flow<List<CatTip>>
}