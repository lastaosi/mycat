package com.lastaosi.mycat.presentation.careguide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lastaosi.mycat.domain.model.BreedMonthlyGuide
import com.lastaosi.mycat.ui.theme.MyCatColors
import com.lastaosi.mycat.ui.theme.MyCatTheme
import org.koin.compose.viewmodel.koinViewModel

// ─── 1. Screen ───────────────────────────────────────────────────────
@Composable
fun CareGuideScreen(
    onBack: () -> Unit,
    viewModel: CareGuideViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    CareGuideContent(
        uiState = uiState,
        onBack = onBack
    )
}

// ─── 2. Content ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareGuideContent(
    uiState: CareGuideUiState,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "케어 가이드",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MyCatColors.OnPrimary
                        )
                        if (uiState.catName.isNotEmpty()) {
                            Text(
                                text = "${uiState.catName} · ${uiState.breedName}",
                                fontSize = 12.sp,
                                color = MyCatColors.OnPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "뒤로",
                            tint = MyCatColors.OnPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MyCatColors.Primary
                )
            )
        },
        containerColor = MyCatColors.Background
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MyCatColors.Primary)
                }
            }
            !uiState.hasBreed -> {
                NoBreedContent()
            }
            uiState.guides.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "가이드 데이터가 없어요",
                        fontSize = 14.sp,
                        color = MyCatColors.TextMuted
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // 현재 월령 하이라이트 카드
                    val currentGuide = uiState.guides.find { it.month == uiState.ageMonth }
                    currentGuide?.let {
                        item {
                            CurrentMonthCard(guide = it, ageMonth = uiState.ageMonth)
                        }
                    }

                    // 전체 월령 리스트
                    item {
                        Text(
                            text = "월령별 가이드",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MyCatColors.OnBackground,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(uiState.guides) { guide ->
                        GuideMonthCard(
                            guide = guide,
                            isCurrent = guide.month == uiState.ageMonth
                        )
                    }
                }
            }
        }
    }
}

// ─── 3. CurrentMonthCard — 현재 월령 강조 카드 ────────────────────────
@Composable
private fun CurrentMonthCard(guide: BreedMonthlyGuide, ageMonth: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MyCatColors.Primary),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🐾 현재 ${ageMonth}개월",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MyCatColors.OnPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GuideChip(label = "건식", value = "${guide.foodDryG}g", modifier = Modifier.weight(1f))
                GuideChip(label = "습식", value = "${guide.foodWetG}g", modifier = Modifier.weight(1f))
                GuideChip(label = "물", value = "${guide.waterMl}ml", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "적정 체중  ${guide.weightMinG / 1000.0}kg — ${guide.weightMaxG / 1000.0}kg",
                fontSize = 12.sp,
                color = MyCatColors.OnPrimary.copy(alpha = 0.85f)
            )
        }
    }
}

// ─── 4. GuideChip ────────────────────────────────────────────────────
@Composable
private fun GuideChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MyCatColors.OnPrimary.copy(alpha = 0.2f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 11.sp, color = MyCatColors.OnPrimary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MyCatColors.OnPrimary
        )
    }
}

// ─── 5. GuideMonthCard — 월령별 카드 ─────────────────────────────────
@Composable
private fun GuideMonthCard(guide: BreedMonthlyGuide, isCurrent: Boolean) {
    val monthLabel = if (guide.month >= 13) {
        val year = guide.month / 12
        val remain = guide.month % 12
        if (remain == 0) "${year}년" else "${year}년 ${remain}개월"
    } else {
        "${guide.month}개월"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MyCatColors.Primary.copy(alpha = 0.08f)
            else MyCatColors.OnPrimary
        ),
        elevation = CardDefaults.cardElevation(if (isCurrent) 2.dp else 1.dp),
        border = if (isCurrent) CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 월령 레이블
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isCurrent) MyCatColors.Primary
                        else MyCatColors.Surface
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = monthLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) MyCatColors.OnPrimary else MyCatColors.TextMuted
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 수치 데이터
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GuideDataItem(label = "건식", value = "${guide.foodDryG}g")
                    GuideDataItem(label = "습식", value = "${guide.foodWetG}g")
                    GuideDataItem(label = "물", value = "${guide.waterMl}ml")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "체중 ${guide.weightMinG / 1000.0}—${guide.weightMaxG / 1000.0}kg",
                    fontSize = 11.sp,
                    color = MyCatColors.TextMuted
                )
            }
        }
    }
}

// ─── 6. GuideDataItem ────────────────────────────────────────────────
@Composable
private fun GuideDataItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = MyCatColors.TextMuted)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MyCatColors.OnBackground
        )
    }
}

// ─── 7. NoBreedContent ───────────────────────────────────────────────
@Composable
private fun NoBreedContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🐱", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "품종을 등록하면 케어 가이드를 볼 수 있어요",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MyCatColors.OnBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "프로필에서 품종을 등록해 주세요",
                fontSize = 12.sp,
                color = MyCatColors.TextMuted
            )
        }
    }
}

// ─── 8. Preview ──────────────────────────────────────────────────────
@Preview(showBackground = true, backgroundColor = 0xFFFFF8F3)
@Composable
private fun CareGuideContentPreview() {
    MyCatTheme {
        CareGuideContent(
            uiState = CareGuideUiState(
                catName = "별이",
                breedName = "코리안 숏헤어",
                ageMonth = 7,
                guides = listOf(
                    BreedMonthlyGuide(1, 1, 1, 500, 800, 20, 10, 80, 5),
                    BreedMonthlyGuide(2, 1, 3, 800, 1200, 30, 15, 120, 8),
                    BreedMonthlyGuide(3, 1, 6, 1500, 2000, 40, 25, 180, 10),
                    BreedMonthlyGuide(4, 1, 7, 1800, 2300, 45, 28, 200, 12),
                    BreedMonthlyGuide(5, 1, 12, 2500, 3500, 55, 35, 250, 15),
                ),
                hasBreed = true,
                isLoading = false
            ),
            onBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F3)
@Composable
private fun CareGuideNoBreedPreview() {
    MyCatTheme {
        CareGuideContent(
            uiState = CareGuideUiState(hasBreed = false),
            onBack = {}
        )
    }
}