package com.lastaosi.mycat.presentation.healthcheck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lastaosi.mycat.domain.model.HealthChecklist
import com.lastaosi.mycat.domain.model.HealthItemType
import com.lastaosi.mycat.ui.theme.MyCatColors
import com.lastaosi.mycat.ui.theme.MyCatTheme
import org.koin.compose.viewmodel.koinViewModel

// ─── 1. Screen ───────────────────────────────────────────────────────
@Composable
fun HealthCheckScreen(
    onBack: () -> Unit,
    viewModel: HealthCheckViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    HealthCheckContent(
        uiState = uiState,
        onBack = onBack,
        onTabSelected = viewModel::onTabSelected
    )
}

// ─── 2. Content ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthCheckContent(
    uiState: HealthCheckUiState,
    onBack: () -> Unit,
    onTabSelected: (HealthCheckTab) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "건강 체크리스트",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MyCatColors.OnPrimary
                        )
                        Text(
                            text = "${uiState.catName} · 현재 ${uiState.ageMonth}개월",
                            fontSize = 12.sp,
                            color = MyCatColors.OnPrimary.copy(alpha = 0.85f)
                        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 탭
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MyCatColors.OnPrimary,
                contentColor = MyCatColors.Primary,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(
                            tabPositions[uiState.selectedTab.ordinal]
                        ),
                        color = MyCatColors.Primary
                    )
                }
            ) {
                HealthCheckTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        text = {
                            Text(
                                text = "${tab.emoji} ${tab.label}",
                                fontSize = 13.sp,
                                fontWeight = if (uiState.selectedTab == tab)
                                    FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MyCatColors.Primary)
                }
            } else if (uiState.filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "✅", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "해당 항목이 없어요",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MyCatColors.OnBackground
                        )
                    }
                }
            } else {
                // 월령별 그룹핑해서 표시
                val grouped = uiState.filteredItems.groupBy { it.month }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    grouped.forEach { (month, items) ->
                        item {
                            MonthHeader(month = month, ageMonth = uiState.ageMonth)
                        }
                        items(items = items, key = { it.id }) { item ->
                            HealthCheckItem(item = item)
                        }
                        item { Spacer(modifier = Modifier.height(4.dp)) }
                    }
                }
            }
        }
    }
}

// ─── 3. MonthHeader ──────────────────────────────────────────────────
@Composable
private fun MonthHeader(month: Int, ageMonth: Int) {
    val isPast = month < ageMonth
    val isCurrent = month == ageMonth

    // 13개월부터 년/개월 표시
    val monthLabel = if (month >= 13) {
        val year = month / 12
        val remainMonth = month % 12
        if (remainMonth == 0) "${year}년" else "${year}년 ${remainMonth}개월"
    } else {
        "${month}개월"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    when {
                        isCurrent -> MyCatColors.Primary
                        isPast    -> MyCatColors.Border
                        else      -> MyCatColors.Surface
                    }
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = monthLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    isCurrent -> MyCatColors.OnPrimary
                    isPast    -> MyCatColors.TextMuted
                    else      -> MyCatColors.Secondary
                }
            )
        }
        if (isCurrent) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "← 현재",
                fontSize = 11.sp,
                color = MyCatColors.Primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─── 4. HealthCheckItem ──────────────────────────────────────────────
@Composable
private fun HealthCheckItem(item: HealthChecklist) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MyCatColors.OnPrimary),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 타입 아이콘
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(item.itemType.backgroundColor()),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.itemType.toEmoji(), fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MyCatColors.OnBackground
                    )
                    if (item.isRecommended) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MyCatColors.Primary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "권장",
                                fontSize = 10.sp,
                                color = MyCatColors.Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = MyCatColors.TextMuted
                )
            }
        }
    }
}

// ─── 5. 확장함수 ─────────────────────────────────────────────────────
private fun HealthItemType.toEmoji(): String = when (this) {
    HealthItemType.VACCINE  -> "💉"
    HealthItemType.CHECK    -> "🏥"
    HealthItemType.SURGERY  -> "✂️"
}

private fun HealthItemType.backgroundColor() = when (this) {
    HealthItemType.VACCINE  -> MyCatColors.Primary.copy(alpha = 0.1f)
    HealthItemType.CHECK    -> MyCatColors.Success.copy(alpha = 0.1f)
    HealthItemType.SURGERY  -> MyCatColors.Secondary.copy(alpha = 0.1f)
}

// ─── 6. Preview ──────────────────────────────────────────────────────
@Preview(showBackground = true, backgroundColor = 0xFFFFF8F3)
@Composable
private fun HealthCheckContentPreview() {
    MyCatTheme {
        HealthCheckContent(
            uiState = HealthCheckUiState(
                catName = "별이",
                ageMonth = 7,
                allItems = listOf(
                    HealthChecklist(1, 2, HealthItemType.VACCINE, "1차 종합백신", "FVRCP 1차 접종", false, true),
                    HealthChecklist(2, 4, HealthItemType.VACCINE, "2차 종합백신", "FVRCP 2차 접종", false, true),
                    HealthChecklist(3, 6, HealthItemType.SURGERY, "중성화 수술", "6개월 이후 권장", false, true),
                    HealthChecklist(4, 7, HealthItemType.CHECK, "정기 건강검진", "체중, 치아, 눈 상태 확인", false, true),
                )
            ),
            onBack = {},
            onTabSelected = {}
        )
    }
}