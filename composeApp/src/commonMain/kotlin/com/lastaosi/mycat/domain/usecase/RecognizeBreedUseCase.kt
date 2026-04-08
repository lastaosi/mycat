package com.lastaosi.mycat.domain.usecase

import com.lastaosi.mycat.data.remote.BreedRecognitionResult
import com.lastaosi.mycat.data.remote.GeminiService
import com.lastaosi.mycat.domain.model.Breed
import com.lastaosi.mycat.domain.repository.BreedRepository
import com.lastaosi.mycat.util.L
import kotlinx.coroutines.flow.first

/**
 * Gemini API로 품종 인식 후 DB 품종과 매칭하는 결과 모델.
 * @param geminiRaw Gemini 원본 응답 품종명
 * @param confidence 인식 신뢰도 (0.0 ~ 1.0)
 * @param matchedBreed DB 품종 매칭 성공 시 Breed, 실패 시 null
 */
data class RecognizeBreedResult(
    val geminiRaw: String,
    val confidence: Double,
    val matchedBreed: Breed?
)

/**
 * 고양이 사진으로 품종을 자동 인식하는 UseCase.
 *
 * 1. GeminiService로 이미지 → 품종명 추출
 * 2. 추출된 품종명으로 BreedRepository 검색 → DB 품종 매칭
 * 3. 매칭 결과를 [RecognizeBreedResult]로 반환
 */
class RecognizeBreedUseCase(
    private val geminiService: GeminiService,
    private val breedRepository: BreedRepository
) {
    suspend operator fun invoke(imageBytes: ByteArray): Result<RecognizeBreedResult> {
        return geminiService.recognizeBreed(imageBytes)
            .mapCatching { result ->
                val matched = findBestMatch(result.breedName)
                RecognizeBreedResult(
                    geminiRaw = result.breedName,
                    confidence = result.confidence,
                    matchedBreed = matched
                )
            }
    }

    private suspend fun findBestMatch(breedName: String): Breed? {
        L.d("findBestMatch 시작 - breedName: $breedName")
        val count = breedRepository.getAllBreeds().first().size
        L.d("breed 테이블 총 개수: $count")
        // 1단계: 전체 이름으로 검색
        val fullMatch = breedRepository.searchBreeds(breedName).first().firstOrNull()
        L.d("1단계 전체 검색 결과: $fullMatch")
        if (fullMatch != null) return fullMatch

        // 2단계: 단어별로 쪼개서 검색
        val words = breedName.split(" ")
            .filter { it.length >= 2 }
            .sortedByDescending { it.length }
        L.d("2단계 검색 단어들: $words")

        for (word in words) {
            val match = breedRepository.searchBreeds(word).first().firstOrNull()
            L.d("단어 '$word' 검색 결과: $match")
            if (match != null) return match
        }

        // 3단계: 앞 2글자로 검색
        val prefix = breedName.take(2)
        val prefixMatch = breedRepository.searchBreeds(prefix).first().firstOrNull()
        L.d("3단계 앞 2글자 '$prefix' 검색 결과: $prefixMatch")

        return prefixMatch
    }
}