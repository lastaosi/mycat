package com.lastaosi.mycat.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastaosi.mycat.domain.model.DiaryMood
import com.lastaosi.mycat.domain.model.MedicationType
import com.lastaosi.mycat.domain.repository.CatRepository
import com.lastaosi.mycat.domain.repository.BreedRepository
import com.lastaosi.mycat.domain.repository.CatDiaryRepository
import com.lastaosi.mycat.domain.repository.CatTipRepository
import com.lastaosi.mycat.domain.repository.MedicationRepository
import com.lastaosi.mycat.domain.repository.VaccinationRecordRepository
import com.lastaosi.mycat.domain.repository.WeightRecordRepository
import com.lastaosi.mycat.domain.usecase.CalculateAgeMonthUseCase
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
 */
class MainViewModel(
    private val catRepository: CatRepository,
    private val breedRepository: BreedRepository,
    private val catTipRepository: CatTipRepository,
    private val calculateAgeMonthUseCase: CalculateAgeMonthUseCase,
    private val vaccinationRecordRepository: VaccinationRecordRepository,
    private val medicationRepository: MedicationRepository,
    private val catDiaryRepository: CatDiaryRepository,
    private val weightRecordRepository: WeightRecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            catRepository.getAllCats()
                .collect { cats ->
                    val representative = cats.firstOrNull { it.isRepresentative }
                        ?: cats.firstOrNull()

                    _uiState.update {
                        it.copy(
                            cat = representative,
                            allCats = cats  // 추가
                        )
                    }
                    representative ?: return@collect

                    val ageMonth = calculateAgeMonthUseCase(representative.birthDate)
                    representative.breedId?.let { breedId ->
                        val guide = breedRepository.getGuideForMonth(breedId, ageMonth)
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
                    // 체중 최신값 추가
                    launch {
                        weightRecordRepository.getWeightHistory(representative.id)
                            .collect { records ->
                                val latest = records.maxByOrNull { it.recordedAt }
                                _uiState.update { it.copy(latestWeightG = latest?.weightG) }
                            }
                    }
                    // 다가오는 예방접종 로드
                    // 추가 데이터 로드
                    loadUpcomingVaccinations()
                    loadUpcomingMedications(representative.id)
                    loadRecentDiaries(representative.id)
                }
        }
    }
    fun onCatSelected(catId: Long) {
        viewModelScope.launch {
            catRepository.setRepresentative(catId)
        }
    }
    fun onMenuClick() {
        // 드로어 열기는 Content에서 drawerState로 직접 처리
        // 필요 시 analytics 등 추가
    }

    fun onDrawerItemClick(item: DrawerItem) {
        _uiState.update { it.copy(selectedDrawerItem = item) }
    }

    fun refreshTip() {
        viewModelScope.launch {
            val tip = catTipRepository.getRandomTip()
            _uiState.update { it.copy(randomTip = tip?.content) }
        }
    }

    private fun loadUpcomingVaccinations() {
        viewModelScope.launch {
            // 현재 시각부터 30일 이내 예방접종 조회
            val now = Clock.System.now().toEpochMilliseconds()
            val records = vaccinationRecordRepository.getUpcomingVaccinations(fromTimestamp = now)
            val alarms = records.map { record ->
                val dDay = calculateDDay(record.nextDueAt ?: 0L)
                UpcomingAlarm(
                    label = record.title,
                    dateLabel = formatDDay(dDay),
                    isUrgent = dDay in 0..3
                )
            }
            _uiState.update { it.copy(upcomingVaccinations = alarms) }
        }
    }

    private fun loadRecentDiaries(catId: Long) {
        viewModelScope.launch {
            // getDiariesByCat Flow에서 최근 2개만 사용
            catDiaryRepository.getDiariesByCat(catId)
                .collect { diaries ->
                    val previews = diaries
                        .sortedByDescending { it.createdAt }
                        .take(2)
                        .map { diary ->
                            DiaryPreview(
                                id = diary.id,
                                title = diary.title ?: "제목 없음",
                                content = diary.content.take(50),
                                mood = diary.mood?.toEmoji(),
                                dateLabel = formatDate(diary.createdAt)
                            )
                        }
                    _uiState.update { it.copy(recentDiaries = previews) }
                }
        }
    }

    // 약 복용 알람 (MedicationAlarm 테이블에서)
    private fun loadUpcomingMedications(catId: Long) {
        viewModelScope.launch {
            medicationRepository.getActiveMedications(catId)
                .collect { medications ->
                    val alarms = medications.map { med ->
                        // 알람 시간은 MedicationAlarm 테이블에 있어서
                        // 여기선 복용 타입으로 표시
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

    // D-Day 계산
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

    private fun formatDate(millis: Long): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${local.year}.${local.monthNumber.toString().padStart(2,'0')}.${local.dayOfMonth.toString().padStart(2,'0')}"
    }
    // DiaryMood → 이모지 변환 확장함수
    private fun DiaryMood.toEmoji(): String = when (this) {
        DiaryMood.HAPPY   -> "😸"
        DiaryMood.NORMAL  -> "😐"
        DiaryMood.SAD     -> "😿"
        DiaryMood.SICK    -> "🤒"
        DiaryMood.PLAYFUL -> "😺"
    }

}