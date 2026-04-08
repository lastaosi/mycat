package com.lastaosi.mycat.presentation

import com.lastaosi.mycat.domain.model.Breed
import com.lastaosi.mycat.domain.model.Cat
import com.lastaosi.mycat.domain.model.Gender
import com.lastaosi.mycat.domain.usecase.RecognizeBreedResult
import com.lastaosi.mycat.domain.usecase.RecognizeBreedUseCase
import com.lastaosi.mycat.domain.usecase.SearchBreedUseCase
import com.lastaosi.mycat.domain.usecase.cat.InsertCatUseCase
import com.lastaosi.mycat.domain.usecase.cat.UpdateCatUseCase
import com.lastaosi.mycat.domain.usecase.cat.GetCatByIdUseCase
import com.lastaosi.mycat.util.L
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock

class ProfileKotlinViewModel : KoinComponent {
    private val scope = MainScope()
    private val insertCat: InsertCatUseCase by inject()
    private val updateCat: UpdateCatUseCase by inject()
    private val searchBreed: SearchBreedUseCase by inject()
    private val recognizeBreed: RecognizeBreedUseCase by inject()
    private val getCatById: GetCatByIdUseCase by inject()

    // 품종 검색
    fun searchBreeds(
        keyword: String,
        onResult: (List<Pair<Int, String>>) -> Unit
    ) {
        scope.launch {
            searchBreed(keyword).collect { breeds ->
                onResult(breeds.map { Pair(it.id, it.nameKo) })
            }
        }
    }

    // Gemini 품종 인식
    fun recognizeBreed(
        imageBytes: ByteArray,
        onResult: (breedName: String,breedId: Int?, confidence: Double) -> Unit,
        onError: () -> Unit
    ) {
        scope.launch {
            try {
                val result = recognizeBreed.invoke(imageBytes)
                result.onSuccess { recognizeBreedResult ->
                    onResult(
                        recognizeBreedResult.matchedBreed?.nameKo ?: recognizeBreedResult.geminiRaw,
                        recognizeBreedResult.matchedBreed?.id,
                        recognizeBreedResult.confidence
                    )
                }.onFailure {
                    onError()
                }

            } catch (e: Exception) {
                onError()
            }
        }
    }

    // 고양이 저장
    fun saveCat(
        catId: Long?,
        name: String,
        birthDate: String,
        gender: String,
        breedId: Int?,
        breedNameCustom: String,
        isNeutered: Boolean,
        memo: String,
        photoPath: String?,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        scope.launch {
            try {
                L.d("saveCat 시작 - catId: $catId, name: $name, birthDate: $birthDate")
                val genderEnum = when (gender) {
                    "MALE" -> Gender.MALE
                    "FEMALE" -> Gender.FEMALE
                    else -> Gender.UNKNOWN
                }
                L.d("gender 변환 완료: $genderEnum")
                val cat = Cat(
                    id = catId ?: 0L,
                    name = name,
                    birthDate = birthDate,
                    gender = genderEnum,
                    breedId = breedId,
                    breedNameCustom = breedNameCustom.ifEmpty { null },
                    isNeutered = isNeutered,
                    memo = memo.ifEmpty { null },
                    photoPath = photoPath,
                    isRepresentative = catId == null,
                    createdAt = Clock.System.now().toEpochMilliseconds()
                )
                L.d("Cat 객체 생성 완료")
                if (catId == null) {
                    insertCat(cat)
                    L.d("insertCat 완료")
                } else {
                    updateCat(cat)
                    L.d("updateCat 완료")
                }
                onSuccess()
            } catch (e: Exception) {
                L.d("saveCat 에러: ${e.message} / ${e.cause}")
                onError()
            }
        }
    }

    fun dispose() {
        scope.cancel()
    }
}