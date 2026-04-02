package com.lastaosi.mycat.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastaosi.mycat.data.remote.GeminiService
import com.lastaosi.mycat.domain.model.Breed
import com.lastaosi.mycat.domain.model.Cat
import com.lastaosi.mycat.domain.model.Gender
import com.lastaosi.mycat.domain.repository.BreedRepository
import com.lastaosi.mycat.domain.repository.CatRepository
import com.lastaosi.mycat.domain.usecase.InsertCatUseCase
import com.lastaosi.mycat.domain.usecase.RecognizeBreedUseCase
import com.lastaosi.mycat.domain.usecase.SearchBreedUseCase
import com.lastaosi.mycat.util.L
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 고양이 등록 화면 UI 상태
 * - breedId: DB 품종 ID (검색/Gemini 인식으로 매칭된 경우), 직접 입력 시 null
 * - breedNameCustom: 화면에 표시되는 품종명 (검색 결과 or 직접 입력)
 * - geminiBreedRaw: Gemini API 원본 응답 품종명 (신뢰도와 함께 표시용)
 * - weightG: 입력 텍스트 (저장 시 kg → g 변환, ×1000)
 */
data class ProfileRegisterUiState(
    val photoPath: String? = null,
    val name: String = "",
    val birthDate: String = "",   // "2023-03"
    val gender: Gender = Gender.UNKNOWN,
    val breedId: Int? = null,
    val breedNameCustom: String = "",
    val geminiBreedRaw: String? = null,
    val geminiConfidence: Double? = null,
    val weightG: String = "",
    val heightCm: String = "",
    val isNeutered: Boolean = false,
    val memo: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val breedSearchQuery: String = "",
    val breedSearchResults: List<Breed> = emptyList(),
    val isBreedSearchVisible: Boolean = false
)

/**
 * 고양이 등록 화면 ViewModel
 *
 * 주요 흐름:
 * 1. 사진 선택 (카메라/갤러리)
 * 2. Gemini API로 품종 자동 인식 → DB 품종과 자동 매칭
 * 3. 품종 직접 검색 (1글자 이상 입력 시 실시간 검색, 최대 5건)
 * 4. 유효성 검사 후 Cat 저장
 */
class ProfileRegisterViewModel(
    private val insertCatUseCase: InsertCatUseCase,
    private val recognizeBreedUseCase: RecognizeBreedUseCase,
    private val searchBreedUseCase: SearchBreedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileRegisterUiState())
    val uiState: StateFlow<ProfileRegisterUiState> = _uiState

    fun onPhotoSelected(path: String) {
        _uiState.update { it.copy(photoPath = path) }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    /**
     * 숫자 입력을 "YYYY-MM" 형식으로 자동 변환.
     * 연도(2000~2026), 월(01~12) 범위를 벗어나면 보정한다.
     */
    fun onBirthDateChanged(input: String) {
        val digits = input.filter { it.isDigit() }.take(6)

        val formatted = if (digits.length >= 5) {
            val year = digits.substring(0, 4).toIntOrNull() ?: 0
            val monthStr = digits.substring(4)
            val month = monthStr.toIntOrNull() ?: 0

            // 월 범위 보정 (1~12)
            val validMonth = when {
                monthStr.length == 2 && month > 12 -> "12"
                monthStr.length == 2 && month < 1 -> "01"
                else -> monthStr
            }

            // 연도 범위 보정 (2000~2026)
            val validYear = when {
                digits.length == 6 && year > 2026 -> "2026"
                digits.length == 6 && year < 2000 -> "2000"
                else -> digits.substring(0, 4)
            }

            "$validYear-$validMonth"
        } else {
            digits
        }

        _uiState.update { it.copy(birthDate = formatted) }
    }

    fun onGenderSelected(gender: Gender) {
        _uiState.update { it.copy(gender = gender) }
    }

    fun onBreedSelected(breedId: Int, breedName: String) {
        _uiState.update { it.copy(breedId = breedId, breedNameCustom = breedName) }
    }

    fun onBreedNameCustomChanged(name: String) {
        _uiState.update { it.copy(breedNameCustom = name, breedId = null) }
    }

    fun onGeminiResult(breedRaw: String, confidence: Double) {
        _uiState.update {
            it.copy(
                geminiBreedRaw = breedRaw,
                breedNameCustom = breedRaw,
                geminiConfidence = confidence,
                breedSearchQuery = breedRaw
            )
        }
    }

    fun onWeightChanged(weight: String) {
        _uiState.update { it.copy(weightG = weight) }
    }

    fun onHeightChanged(height: String) {
        _uiState.update { it.copy(heightCm = height) }
    }

    fun onNeuteredChanged(isNeutered: Boolean) {
        _uiState.update { it.copy(isNeutered = isNeutered) }
    }

    fun onMemoChanged(memo: String) {
        _uiState.update { it.copy(memo = memo) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    @OptIn(ExperimentalTime::class)
    fun save() {
        val state = _uiState.value
        if (state.photoPath == null) {
            _uiState.update { it.copy(errorMessage = "고양이 사진을 등록해주세요") }
            return
        }
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "이름을 입력해주세요") }
            return
        }
        if (state.birthDate.isBlank()) {
            _uiState.update { it.copy(errorMessage = "생년월을 입력해주세요") }
            return
        }
        if (!isValidBirthDate(state.birthDate)) {
            _uiState.update { it.copy(errorMessage = "생년월 형식이 올바르지 않아요 (예: 2023-03)") }
            return
        }
        if (state.breedId == null && state.breedNameCustom.isBlank()) {
            _uiState.update { it.copy(errorMessage = "품종을 검색하거나 직접 입력해주세요") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                insertCatUseCase(
                    Cat(
                        name = state.name,
                        birthDate = state.birthDate,
                        gender = state.gender,
                        breedId = state.breedId,
                        breedNameCustom = state.breedNameCustom.ifBlank { null },
                        geminiBreedRaw = state.geminiBreedRaw,
                        geminiConfidence = state.geminiConfidence,
                        weightG = state.weightG.toIntOrNull()?.times(1000),
                        heightCm = state.heightCm.toDoubleOrNull(),
                        photoPath = state.photoPath,
                        isNeutered = state.isNeutered,
                        isRepresentative = true,
                        memo = state.memo.ifBlank { null },
                        createdAt = Clock.System.now().toEpochMilliseconds()
                    )
                )
                // 저장 후 DB 확인용 로그
                _uiState.update { it.copy(isSaved = true, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "저장 중 오류가 발생했습니다.")
                }
            }
        }
    }

    /**
     * Gemini API로 품종 인식 후 DB 품종과 자동 매칭.
     * 인식 성공 시 breedId, breedNameCustom, breedSearchQuery를 함께 업데이트.
     */
    fun recognizeBreed(imageBytes: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            recognizeBreedUseCase(imageBytes)
                .onSuccess { result ->
                    L.d("Gemini 인식 결과: ${result.geminiRaw}")
                    L.d("DB 매칭 결과: ${result.matchedBreed}")

                    _uiState.update {
                        it.copy(
                            geminiBreedRaw = result.geminiRaw,
                            breedNameCustom = result.geminiRaw,
                            geminiConfidence = result.confidence,
                            breedSearchQuery = result.geminiRaw,
                            breedId = result.matchedBreed?.id,
                            isLoading = false
                        )
                    }
                }
                .onFailure { e ->
                    L.e(e.message)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "품종 인식 실패: ${e.message}"
                        )
                    }
                }
        }
    }

    fun onBreedSearchQueryChanged(query: String) {
        _uiState.update { it.copy(breedSearchQuery = query) }
        if (query.length >= 1) {
            searchBreeds(query)
        } else {
            _uiState.update { it.copy(breedSearchResults = emptyList()) }
        }
    }

    fun onBreedSearchVisibilityChanged(visible: Boolean) {
        _uiState.update { it.copy(isBreedSearchVisible = visible) }
    }

    private fun searchBreeds(query: String) {
        viewModelScope.launch {
            searchBreedUseCase(query)
                .collect { breeds ->
                    _uiState.update { it.copy(breedSearchResults = breeds) }
                }
        }
    }

    fun onBreedSelected(breed: Breed) {
        _uiState.update {
            it.copy(
                breedId = breed.id,
                breedNameCustom = breed.nameKo,
                breedSearchQuery = breed.nameKo,
                breedSearchResults = emptyList(),
                isBreedSearchVisible = false
            )
        }
    }

    private fun isValidBirthDate(birthDate: String): Boolean {
        return try {
            val parts = birthDate.split("-")
            if (parts.size != 2) return false
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            // 연도: 2000~현재년도, 월: 1~12
            year in 2000..2026 && month in 1..12
        } catch (e: Exception) {
            false  // 예외 발생 시 false 반환 (크래시 방지)
        }
    }
}