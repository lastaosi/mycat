package com.lastaosi.mycat.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastaosi.mycat.domain.model.DiaryMood
import com.lastaosi.mycat.domain.model.MedicationType
import com.lastaosi.mycat.domain.usecase.MainUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * 메인 화면 ViewModel.
 *
 * 초기화 시 대표 고양이를 로드하고, 해당 고양이의 월령에 맞는
 * 오늘의 급여량·체중 범위(품종 가이드), 다가오는 예방접종/투약 알람,
 * 최근 일기 미리보기를 [MainUiState]로 관리한다.
 *
 * ## 데이터 로드 구조
 * [loadData]는 `getAllCats()` Flow 를 구독해 대표 고양이가 바뀔 때마다 자동 갱신된다.
 * 대표 고양이 확정 후 부가 데이터(체중/예방접종/약/일기/건강검진/팁)는
 * 별도 `launch` 로 병렬 로드한다.
 *
 * ## D-Day 계산 ([calculateDDay])
 * `(nextDueAt - now) / 86_400_000` 으로 일(day) 단위 차이를 구한다.
 * - 양수: 남은 일수 → "D-N"
 * - 0   : 당일 → "오늘"
 * - 음수: 기한 초과 → "기한 지남"
 * isUrgent = dDay in 0..3 (당일~3일 이내를 긴급으로 표시)
 */
class MainViewModel(
    private val useCase: MainUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    init {
        loadData()
    }

    // 기존 개별 함수들 대신 onAction 하나로
    fun onAction(action: MainAction) {
        when (action) {
            is MainAction.MenuClick -> { /* 드로어는 Content에서 처리 */ }
            is MainAction.RefreshTip -> refreshTip()
            is MainAction.AddCatClick -> { /* Navigation은 Screen에서 처리 */ }
            is MainAction.CatSelected -> onCatSelected(action.catId)
            is MainAction.DrawerItemClick -> onDrawerItemClick(action.item)
            is MainAction.Navigate -> { /* Navigation은 Screen에서 처리 */ }
            else -> {}
        }
    }

    private fun onCatSelected(catId: Long) {
        viewModelScope.launch {
            useCase.setRepresentative(catId)
        }
    }

    private fun onDrawerItemClick(item: DrawerItem) {
        _uiState.update { it.copy(selectedDrawerItem = item) }
    }

    private fun loadData() {
        viewModelScope.launch {
            // getAllCats Flow: 대표 고양이가 바뀔 때마다 전체 재로드
            useCase.getAllCats()
                .collect { cats ->
                    val representative = cats.firstOrNull { it.isRepresentative }
                        ?: cats.firstOrNull()

                    _uiState.update { it.copy(cat = representative, allCats = cats) }
                    representative ?: return@collect

                    val ageMonth = useCase.calculateAgeMonth(representative.birthDate)

                    // 품종이 등록된 경우에만 급여 가이드 조회
                    representative.breedId?.let { breedId ->
                        val guide = useCase.getBreedGuide(breedId, ageMonth)
                        _uiState.update {
                            it.copy(
                                todayFoodDryG = guide?.foodDryG ?: 0,
                                todayFoodWetG = guide?.foodWetG ?: 0,
                                todayWaterMl  = guide?.waterMl  ?: 0,
                                weightMinG    = guide?.weightMinG ?: 0,
                                weightMaxG    = guide?.weightMaxG ?: 0,
                            )
                        }
                    }

                    // 체중 히스토리 Flow 구독 — 새 기록 추가 시 자동 갱신
                    launch {
                        useCase.getLatestWeight(representative.id)
                            .collect { records ->
                                val latest = records.maxByOrNull { it.recordedAt }
                                _uiState.update { it.copy(latestWeightG = latest?.weightG) }
                            }
                    }

                    // 부가 데이터는 별도 launch 로 병렬 로드 (UI 블로킹 없음)
                    loadUpcomingVaccinations()
                    loadUpcomingMedications(representative.id)
                    loadRecentDiaries(representative.id)
                    loadHealthCheckSummary(ageMonth)
                    refreshTip()
                }
        }
    }

    fun onMenuClick() {
        // 드로어 열기는 Content에서 drawerState로 직접 처리
        // 필요 시 analytics 등 추가
    }

    private fun loadHealthCheckSummary(ageMonth: Int) {
        viewModelScope.launch {
            useCase.getHealthCheckSummary(ageMonth)
                .collect { items ->
                    _uiState.update { it.copy(healthCheckItems = items) }
                }
        }
    }
    fun refreshTip() {
        viewModelScope.launch {
            val tip = useCase.getRandomTip()
            _uiState.update { it.copy(randomTip = tip?.content) }
        }
    }

    /** 예정된 예방접종을 로드해 D-Day 레이블로 변환한다. */
    private fun loadUpcomingVaccinations() {
        viewModelScope.launch {
            // fromTimestamp = now → 현재 이후 예정된 접종만 조회
            val now = Clock.System.now().toEpochMilliseconds()
            val records = useCase.getUpcomingVaccinations(fromTimestamp = now)
            val alarms = records.map { record ->
                val dDay = calculateDDay(record.nextDueAt ?: 0L)
                UpcomingAlarm(
                    label = record.title,
                    dateLabel = formatDDay(dDay),
                    isUrgent = dDay in 0..3  // 당일~3일 이내는 긴급 표시
                )
            }
            _uiState.update { it.copy(upcomingVaccinations = alarms) }
        }
    }

    /** 최근 일기 2개를 로드해 미리보기용 [DiaryPreview] 로 변환한다. */
    private fun loadRecentDiaries(catId: Long) {
        viewModelScope.launch {
            useCase.getDiaries(catId)
                .collect { diaries ->
                    val previews = diaries
                        .sortedByDescending { it.createdAt }
                        .take(2)
                        .map { diary ->
                            DiaryPreview(
                                id = diary.id,
                                title = diary.title ?: "제목 없음",
                                content = diary.content.take(50),  // 50자 미리보기
                                mood = diary.mood?.toEmoji(),
                                dateLabel = formatDate(diary.createdAt)
                            )
                        }
                    _uiState.update { it.copy(recentDiaries = previews) }
                }
        }
    }

    /**
     * 활성 복용 약 목록을 로드한다.
     * 개별 알람 시각은 MedicationAlarm 테이블에 있으므로
     * 메인 화면에서는 복용 타입 레이블만 표시한다.
     */
    private fun loadUpcomingMedications(catId: Long) {
        viewModelScope.launch {
            useCase.getMedications.active(catId)
                .collect { medications ->
                    val alarms = medications.map { med ->
                        val typeLabel = when (med.medicationType) {
                            MedicationType.ONCE     -> "1회"
                            MedicationType.DAILY    -> "매일"
                            MedicationType.INTERVAL -> "${med.intervalDays}일마다"
                            MedicationType.PERIOD   -> "기간 복용"
                        }
                        UpcomingAlarm(
                            label = med.name,
                            dateLabel = typeLabel,
                            isUrgent = false
                        )
                    }
                    _uiState.update { it.copy(upcomingMedications = alarms) }
                }
        }
    }

    /**
     * D-Day 를 계산한다.
     * 밀리초 차이를 하루(86_400_000ms)로 나눈 정수값.
     * 양수 = 남은 일수, 0 = 당일, 음수 = 기한 초과.
     */
    private fun calculateDDay(dateMillis: Long): Int {
        val now = Clock.System.now().toEpochMilliseconds()
        val diff = dateMillis - now
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }

    private fun formatDDay(dDay: Int): String = when {
        dDay < 0  -> "기한 지남"
        dDay == 0 -> "오늘"
        else      -> "D-$dDay"
    }

    /** epoch ms → "yyyy.MM.dd" 형식 (kotlinx-datetime 사용, 로컬 TimeZone 기준) */
    private fun formatDate(millis: Long): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${local.year}.${local.monthNumber.toString().padStart(2,'0')}.${local.dayOfMonth.toString().padStart(2,'0')}"
    }

    /** [DiaryMood] enum → 이모지 문자열 변환 확장함수 */
    private fun DiaryMood.toEmoji(): String = when (this) {
        DiaryMood.HAPPY   -> "😸"
        DiaryMood.NORMAL  -> "😐"
        DiaryMood.SAD     -> "😿"
        DiaryMood.SICK    -> "🤒"
        DiaryMood.PLAYFUL -> "😺"
    }
}