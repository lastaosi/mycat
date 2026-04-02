package com.lastaosi.mycat.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lastaosi.mycat.db.MyCatDatabase
import com.lastaosi.mycat.domain.model.CatTip
import com.lastaosi.mycat.domain.model.CatTipCategory
import com.lastaosi.mycat.domain.repository.CatTipRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CatTipRepositoryImpl(
    private val db: MyCatDatabase
) : CatTipRepository {

    override fun getAllTips(): Flow<List<CatTip>> =
        db.catTipQueries.getAllTips()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getRandomTip(): CatTip? =
        withContext(Dispatchers.IO) {
            db.catTipQueries.getRandomTip().executeAsOneOrNull()?.toDomain()
        }

    override suspend fun getRandomTipByCategory(category: CatTipCategory): CatTip? =
        withContext(Dispatchers.IO) {
            db.catTipQueries.getRandomTipByCategory(category.name)
                .executeAsOneOrNull()?.toDomain()
        }

    override fun getTipsByCategory(category: CatTipCategory): Flow<List<CatTip>> =
        db.catTipQueries.getTipsByCategory(category.name)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }

    private fun com.lastaosi.mycat.db.Cat_tip.toDomain() = CatTip(
        id = id,
        content = content,
        category = runCatching { CatTipCategory.valueOf(category) }
            .getOrDefault(CatTipCategory.HEALTH)
    )
}