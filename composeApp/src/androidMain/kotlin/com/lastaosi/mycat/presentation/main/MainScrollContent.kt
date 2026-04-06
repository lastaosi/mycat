package com.lastaosi.mycat.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lastaosi.mycat.domain.model.Cat
import com.lastaosi.mycat.domain.model.Gender
import com.lastaosi.mycat.ui.theme.MyCatColors
import com.lastaosi.mycat.ui.theme.MyCatTheme

// ─── 1. MainScrollContent ────────────────────────────────────────────
@Composable
fun MainScrollContent(
    uiState: MainUiState,
    onAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MyCatColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 고양이 없을 때
        if (uiState.cat == null) {
            NoCatCard()
            return@Column
        }

        // 오늘의 케어 가이드 카드
        CareGuideCard(
            cat = uiState.cat,
            foodDryG = uiState.todayFoodDryG,
            foodWetG = uiState.todayFoodWetG,
            waterMl = uiState.todayWaterMl,
            weightMinG = uiState.weightMinG,
            weightMaxG = uiState.weightMaxG
        )

        // 건강 체크리스트 카드
        HealthCheckCard()

        // 최근 체중 카드
        LatestWeightCard(
            weightG = uiState.latestWeightG,
            weightMinG = uiState.weightMinG,
            weightMaxG = uiState.weightMaxG
        )
// 다가오는 알림 카드
        UpcomingAlarmsCard(
            vaccinations = uiState.upcomingVaccinations,
            medications = uiState.upcomingMedications
        )

// 최근 다이어리 카드
        if (uiState.recentDiaries.isNotEmpty()) {
            RecentDiaryCard(diaries = uiState.recentDiaries)
        }


        // 전광판 팁 배너
        uiState.randomTip?.let { tip ->
            TipBannerCard(tip = tip,
                onTap = { onAction(MainAction.RefreshTip) }
            )
        }

        // AdMob 배너 자리
        AdMobPlaceholder()

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─── 2. CareGuideCard ────────────────────────────────────────────────
@Composable
private fun CareGuideCard(
    cat: Cat,
    foodDryG: Int,
    foodWetG: Int,
    waterMl: Int,
    weightMinG: Int,
    weightMaxG: Int
) {
    MainCard {
        // 헤더
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "🐾", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "오늘의 케어 가이드",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MyCatColors.OnBackground
                )
                Text(
                    text = "${cat.name} · ${cat.breedNameCustom ?: "품종 미등록"}",
                    fontSize = 12.sp,
                    color = MyCatColors.TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 급여량 칩 행
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CareChip(label = "건식", value = "${foodDryG}g")
            CareChip(label = "습식", value = "${foodWetG}g")
            CareChip(label = "물", value = "${waterMl}ml")
        }

        if (weightMinG > 0 && weightMaxG > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "적정 체중  ${weightMinG / 1000.0}kg — ${weightMaxG / 1000.0}kg",
                fontSize = 12.sp,
                color = MyCatColors.TextMuted
            )
        }
    }
}

@Composable
private fun CareChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MyCatColors.Surface)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MyCatColors.TextMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MyCatColors.Secondary
        )
    }
}

// ─── 3. HealthCheckCard ──────────────────────────────────────────────
@Composable
private fun HealthCheckCard() {
    MainCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "❤️", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "건강 체크리스트",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MyCatColors.OnBackground
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // TODO: DB 연동 후 실제 데이터로 교체
        listOf(
            Pair("3차 예방접종", false),
            Pair("정기 검진", true),
            Pair("구충제 복용", false)
        ).forEach { (label, isDone) ->
            HealthCheckRow(label = label, isDone = isDone)
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun HealthCheckRow(label: String, isDone: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (isDone) MyCatColors.Success
                    else MyCatColors.Border
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Text(text = "✓", fontSize = 11.sp, color = MyCatColors.OnPrimary)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (isDone) MyCatColors.TextMuted else MyCatColors.OnBackground,
            textDecoration = if (isDone)
                androidx.compose.ui.text.style.TextDecoration.LineThrough
            else null
        )
    }
}

// ─── 4. LatestWeightCard ─────────────────────────────────────────────
@Composable
private fun LatestWeightCard(
    weightG: Int?,
    weightMinG: Int,
    weightMaxG: Int
) {
    MainCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "⚖️", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "최근 체중",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MyCatColors.OnBackground
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (weightG != null) {
            val weightKg = weightG / 1000.0
            val isNormal = weightG in weightMinG..weightMaxG
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${weightKg}kg",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MyCatColors.OnBackground
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isNormal) MyCatColors.Success.copy(alpha = 0.15f)
                            else MyCatColors.Primary.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isNormal) "정상 범위" else "범위 벗어남",
                        fontSize = 12.sp,
                        color = if (isNormal) MyCatColors.Success else MyCatColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Text(
                text = "아직 체중 기록이 없어요",
                fontSize = 13.sp,
                color = MyCatColors.TextMuted
            )
        }
    }
}



// ─── 5. TipBannerCard ────────────────────────────────────────────────
@Composable
private fun TipBannerCard(tip: String,onTap: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MyCatColors.Primary)
            .clickable { onTap() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "💡", fontSize = 16.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = tip,
            fontSize = 13.sp,
            color = MyCatColors.OnPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

// ─── 6. AdMob Placeholder ────────────────────────────────────────────
@Composable
private fun AdMobPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MyCatColors.Border.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "광고 영역",
            fontSize = 12.sp,
            color = MyCatColors.TextMuted
        )
    }
}

// ─── 7. NoCatCard ────────────────────────────────────────────────────
@Composable
private fun NoCatCard() {
    MainCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🐱", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "고양이를 등록해주세요",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MyCatColors.OnBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "프로필 등록 후 케어 가이드를 확인할 수 있어요",
                fontSize = 12.sp,
                color = MyCatColors.TextMuted
            )
        }
    }
}

// ─── 8. 공통 카드 래퍼 ───────────────────────────────────────────────
@Composable
private fun MainCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MyCatColors.OnPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
// ─── UpcomingAlarmsCard ───────────────────────────────────────────
@Composable
private fun UpcomingAlarmsCard(
    vaccinations: List<UpcomingAlarm>,
    medications: List<UpcomingAlarm>
) {
    val allAlarms = vaccinations + medications
    MainCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "🔔", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "다가오는 알림",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MyCatColors.OnBackground
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (allAlarms.isEmpty()) {
            Text(
                text = "예정된 알림이 없어요",
                fontSize = 13.sp,
                color = MyCatColors.TextMuted
            )
        } else {
            allAlarms.forEach { alarm ->
                AlarmRow(alarm = alarm)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun AlarmRow(alarm: UpcomingAlarm) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = alarm.label,
            fontSize = 13.sp,
            color = MyCatColors.OnBackground
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (alarm.isUrgent) MyCatColors.Primary.copy(alpha = 0.15f)
                    else MyCatColors.Surface
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = alarm.dateLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (alarm.isUrgent) MyCatColors.Primary
                else MyCatColors.TextMuted
            )
        }
    }
}

// ─── RecentDiaryCard ─────────────────────────────────────────────
@Composable
private fun RecentDiaryCard(diaries: List<DiaryPreview>) {
    MainCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "📝", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "최근 다이어리",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MyCatColors.OnBackground
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        diaries.forEachIndexed { index, diary ->
            DiaryPreviewRow(diary = diary)
            if (index < diaries.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MyCatColors.Border,
                    thickness = 0.5.dp
                )
            }
        }
    }
}

@Composable
private fun DiaryPreviewRow(diary: DiaryPreview) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                diary.mood?.let {
                    Text(text = it, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = diary.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MyCatColors.OnBackground
                )
            }
            Text(
                text = diary.dateLabel,
                fontSize = 11.sp,
                color = MyCatColors.TextMuted
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = diary.content,
            fontSize = 12.sp,
            color = MyCatColors.TextMuted,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
// ─── 9. Preview ──────────────────────────────────────────────────────
@Preview(showBackground = true, backgroundColor = 0xFFFFF8F3)
@Composable
private fun MainScrollContentPreview() {
    MyCatTheme {
        MainScrollContent(
            uiState = MainUiState(
                cat = Cat(
                    id = 1L,
                    name = "별이",
                    birthDate = "2024-09",
                    gender = Gender.FEMALE,
                    breedNameCustom = "코리안 숏헤어",
                    isNeutered = true,
                    isRepresentative = true,
                    createdAt = 0L
                ),
                todayFoodDryG = 45,
                todayFoodWetG = 30,
                todayWaterMl = 210,
                weightMinG = 2100,
                weightMaxG = 2400,
                latestWeightG = 2300,
                randomTip = "매달 구충제로 심장사상충과 기생충 방어하기",

                // 여기 추가
                upcomingVaccinations = listOf(
                    UpcomingAlarm("3차 종합백신", "D-3", isUrgent = true),
                    UpcomingAlarm("광견병 예방접종", "D-14", isUrgent = false)
                ),
                upcomingMedications = listOf(
                    UpcomingAlarm("항생제", "오늘 18:00", isUrgent = true)
                ),
                recentDiaries = listOf(
                    DiaryPreview(1L, "오늘의 별이", "오늘 레오가 새벽에 기세차게...", "😸", "2026.04.01"),
                    DiaryPreview(2L, "병원 다녀온 날", "정기검진 결과 건강하다고 해서 다행이었다.", "😊", "2026.03.28")
                )
            ),
            onAction = {}
        )
    }
}


@Preview(showBackground = true, backgroundColor = 0xFFFFF8F3)
@Composable
private fun MainScrollContentNoCatPreview() {
    MyCatTheme {
        MainScrollContent(uiState = MainUiState(),
            onAction = {}  )
    }
}