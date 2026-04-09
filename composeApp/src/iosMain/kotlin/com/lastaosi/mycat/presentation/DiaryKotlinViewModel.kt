package com.lastaosi.mycat.presentation

import com.lastaosi.mycat.domain.model.CatDiary
import com.lastaosi.mycat.domain.model.DiaryMood
import com.lastaosi.mycat.domain.usecase.diary.DiaryUseCase
import com.lastaosi.mycat.util.L
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * iOS 전용 다이어리 KotlinViewModel.
 *
 * Android DiaryViewModel 과 동일한 비즈니스 로직을 수행하며,
 * suspend / Flow → callback 패턴으로 Swift 에 데이터를 전달한다.
 *
 * 사용 패턴:
 *  1. loadData() 로 catId 를 받아 고양이 정보 + 다이어리 목록 Flow 구독 시작
 *  2. saveDiary() 로 신규 저장 또는 수정
 *  3. deleteDiary() 로 삭제
 *  4. deinit 시 dispose() 호출 → 코루틴 스코프 취소
 */
class DiaryKotlinViewModel : KoinComponent {

    private val scope = MainScope()
    private val useCase: DiaryUseCase by inject()

    // ── 현재 catId ──────────────────────────────────────────────────────────
    private var currentCatId: Long = 0L

    // ────────────────────────────────────────────────────────────────────────
    // MARK: - Load
    // ────────────────────────────────────────────────────────────────────────

    /**
     * catId 에 해당하는 고양이 정보와 다이어리 목록을 로드한다.
     *
     * @param catId          로드할 고양이 ID
     * @param onCatLoaded    고양이 이름 로드 완료 콜백
     * @param onDiaries      다이어리 목록 업데이트 콜백 (createdAt 내림차순 정렬됨)
     */
    fun loadData(
        catId: Long,
        onCatLoaded: (catName: String) -> Unit,
        onDiaries: (diaries: List<CatDiary>) -> Unit
    ) {
        currentCatId = catId
        scope.launch {
            val cat = useCase.getCatById(catId)
            if (cat == null) {
                L.d("DiaryKotlinViewModel: cat not found for id=$catId")
                return@launch
            }
            onCatLoaded(cat.name)
            L.d("DiaryKotlinViewModel: cat loaded name=${cat.name}")

            useCase.getDiaries(catId).collect { list ->
                val sorted = list.sortedByDescending { it.createdAt }
                L.d("DiaryKotlinViewModel: diaries updated size=${sorted.size}")
                onDiaries(sorted)
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // MARK: - Save (신규 / 수정)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * 다이어리를 저장(신규 또는 수정)한다.
     *
     * @param diaryId       수정 시 기존 ID (신규면 0L)
     * @param title         제목 (없으면 빈 문자열 → null 저장)
     * @param content       내용 (필수)
     * @param moodRaw       기분 문자열 ("HAPPY" | "NORMAL" | "SAD" | "SICK" | "PLAYFUL" | "")
     * @param photoPath     사진 경로 (없으면 null)
     * @param dateMillis    날짜 epoch ms
     * @param onComplete    저장 완료 콜백
     */
    @OptIn(ExperimentalTime::class)
    fun saveDiary(
        diaryId: Long,
        title: String,
        content: String,
        moodRaw: String,
        photoPath: String?,
        dateMillis: Long,
        onComplete: () -> Unit
    ) {
        val mood = parseMood(moodRaw)
        val now = Clock.System.now().toEpochMilliseconds()

        scope.launch {
            if (diaryId != 0L) {
                // ── 수정 모드 ──────────────────────────────────────────────
                useCase.saveDiary(
                    CatDiary(
                        id = diaryId,
                        catId = currentCatId,
                        title = title.ifBlank { null },
                        content = content,
                        mood = mood,
                        photoPath = photoPath,
                        createdAt = dateMillis,
                        updatedAt = now
                    )
                )
                L.d("DiaryKotlinViewModel: updated diary id=$diaryId")
            } else {
                // ── 신규 모드 ──────────────────────────────────────────────
                val newId = useCase.saveDiary(
                    CatDiary(
                        catId = currentCatId,
                        title = title.ifBlank { null },
                        content = content,
                        mood = mood,
                        photoPath = photoPath,
                        createdAt = dateMillis,
                        updatedAt = now
                    )
                )
                L.d("DiaryKotlinViewModel: inserted diary id=$newId")
            }
            onComplete()
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // MARK: - Delete
    // ────────────────────────────────────────────────────────────────────────

    /**
     * 다이어리를 삭제한다.
     *
     * @param diaryId    삭제할 다이어리 ID
     * @param onComplete 삭제 완료 콜백
     */
    fun deleteDiary(
        diaryId: Long,
        onComplete: () -> Unit
    ) {
        scope.launch {
            useCase.deleteDiary(diaryId)
            L.d("DiaryKotlinViewModel: deleted diary id=$diaryId")
            onComplete()
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // MARK: - Helpers
    // ────────────────────────────────────────────────────────────────────────

    private fun parseMood(raw: String): DiaryMood? =
        when (raw.uppercase()) {
            "HAPPY"   -> DiaryMood.HAPPY
            "NORMAL"  -> DiaryMood.NORMAL
            "SAD"     -> DiaryMood.SAD
            "SICK"    -> DiaryMood.SICK
            "PLAYFUL" -> DiaryMood.PLAYFUL
            else      -> null
        }

    /** 코루틴 스코프 취소. Swift deinit 에서 반드시 호출해야 한다. */
    fun dispose() {
        scope.cancel()
        L.d("DiaryKotlinViewModel: disposed")
    }
}
