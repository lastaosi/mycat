package com.lastaosi.mycat.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lastaosi.mycat.db.MyCatDatabase
import com.lastaosi.mycat.domain.model.Cat
import com.lastaosi.mycat.domain.model.Gender
import com.lastaosi.mycat.domain.repository.CatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * SQLDelight 기반 CatRepository 구현체.
 * - Flow 반환: asFlow() + mapToList(Dispatchers.IO)
 * - suspend 함수: withContext(Dispatchers.IO)
 * - DB 타입 변환: Long → Int, Long(1/0) → Boolean
 */
class CatRepositoryImpl(
    private val db: MyCatDatabase
) : CatRepository {

    override fun getAllCats(): Flow<List<Cat>> =
        db.catQueries.getAllCats()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getCatById(id: Long): Cat? =
        withContext(Dispatchers.IO) {
            db.catQueries.getCatById(id).executeAsOneOrNull()?.toDomain()
        }

    override suspend fun getRepresentativeCat(): Cat? =
        withContext(Dispatchers.IO) {
            db.catQueries.getRepresentativeCat().executeAsOneOrNull()?.toDomain()
        }

    override suspend fun insert(cat: Cat): Unit =
        withContext(Dispatchers.IO) {
            db.catQueries.insert(
                name = cat.name,
                birthDate = cat.birthDate,
                gender = cat.gender.name,
                breedId = cat.breedId?.toLong(),
                breedNameCustom = cat.breedNameCustom,
                geminiBreedRaw = cat.geminiBreedRaw,
                geminiConfidence = cat.geminiConfidence,
                weightG = cat.weightG?.toLong(),
                heightCm = cat.heightCm,
                photoPath = cat.photoPath,
                isNeutered = if (cat.isNeutered) 1L else 0L,
                isRepresentative = if (cat.isRepresentative) 1L else 0L,
                memo = cat.memo,
                createdAt = cat.createdAt
            )
            Unit
        }

    override suspend fun update(cat: Cat): Unit =
        withContext(Dispatchers.IO) {
            db.catQueries.update(
                id = cat.id,
                name = cat.name,
                birthDate = cat.birthDate,
                gender = cat.gender.name,
                breedId = cat.breedId?.toLong(),
                breedNameCustom = cat.breedNameCustom,
                geminiBreedRaw = cat.geminiBreedRaw,
                geminiConfidence = cat.geminiConfidence,
                weightG = cat.weightG?.toLong(),
                heightCm = cat.heightCm,
                photoPath = cat.photoPath,
                isNeutered = if (cat.isNeutered) 1L else 0L,
                isRepresentative = if (cat.isRepresentative) 1L else 0L,
                memo = cat.memo
            )
            Unit
        }

    override suspend fun delete(cat: Cat): Unit =
        withContext(Dispatchers.IO) {
            db.catQueries.delete(cat.id)
            Unit
        }

    override suspend fun setRepresentative(id: Long) : Unit=
        withContext(Dispatchers.IO) {
            db.catQueries.setRepresentative(id)
            Unit
        }

    override suspend fun getCount(): Long =
        withContext(Dispatchers.IO) {
            db.catQueries.getCount().executeAsOne()
        }

    private fun com.lastaosi.mycat.db.Cat.toDomain() = Cat(
        id = id,
        name = name,
        birthDate = birthDate,
        gender = runCatching { Gender.valueOf(gender) }.getOrDefault(Gender.UNKNOWN),
        breedId = breedId?.toInt(),
        breedNameCustom = breedNameCustom,
        geminiBreedRaw = geminiBreedRaw,
        geminiConfidence = geminiConfidence,
        weightG = weightG?.toInt(),
        heightCm = heightCm,
        photoPath = photoPath,
        isNeutered = isNeutered == 1L,
        isRepresentative = isRepresentative == 1L,
        memo = memo,
        createdAt = createdAt
    )
}
