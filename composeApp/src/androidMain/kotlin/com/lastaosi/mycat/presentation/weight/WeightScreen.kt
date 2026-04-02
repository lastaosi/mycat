package com.lastaosi.mycat.presentation.weight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// 기존 2.x import 전부 제거
// 아래로 교체
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.lastaosi.mycat.domain.model.WeightRecord
import com.lastaosi.mycat.ui.theme.MyCatColors
import com.lastaosi.mycat.ui.theme.MyCatTheme
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import com.patrykandpatrick.vico.compose.component.textComponent

// ─── 1. Screen ───────────────────────────────────────────────────────
@Composable
fun WeightScreen(
    catId: Long,
    onBack: () -> Unit,
    viewModel: WeightViewModel = koinViewModel()
) {
    // catId가 바뀔 때마다 데이터 재로드
    LaunchedEffect(catId) {
        viewModel.loadData(catId)
    }

    val uiState by viewModel.uiState.collectAsState()
    WeightContent(
        uiState = uiState,
        onBack = onBack,
        onTabSelected = viewModel::onTabSelected,
        onFabClick = viewModel::onFabClick,
        onDialogDismiss = viewModel::onDialogDismiss,
        onWeightSave = viewModel::onWeightSave
    )
}

// ─── 2. Content ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightContent(
    uiState: WeightUiState,
    onBack: () -> Unit,
    onTabSelected: (WeightTab) -> Unit,
    onFabClick: () -> Unit,
    onDialogDismiss: () -> Unit,
    onWeightSave: (String, String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "체중 기록",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MyCatColors.OnPrimary
                        )
                        Text(
                            text = uiState.catName,
                            fontSize = 12.sp,
                            color = MyCatColors.OnPrimary.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onFabClick,
                containerColor = MyCatColors.Primary,
                contentColor = MyCatColors.OnPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "체중 추가")
            }
        },
        containerColor = MyCatColors.Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 탭
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MyCatColors.OnPrimary,
                contentColor = MyCatColors.Primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(
                            tabPositions[uiState.selectedTab.ordinal]
                        ),
                        color = MyCatColors.Primary
                    )
                }
            ) {
                WeightTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        text = {
                            Text(
                                text = tab.label,
                                fontSize = 13.sp,
                                fontWeight = if (uiState.selectedTab == tab)
                                    FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // 탭 컨텐츠
            when (uiState.selectedTab) {
                WeightTab.MY_CAT -> MyWeightTab(uiState = uiState)
                WeightTab.BREED_AVERAGE -> BreedAverageTab(uiState = uiState)
            }
        }
    }

    // 체중 입력 다이얼로그
    if (uiState.showInputDialog) {
        WeightInputDialog(
            onDismiss = onDialogDismiss,
            onSave = onWeightSave
        )
    }
}

// ─── 3. MyWeightTab ──────────────────────────────────────────────────
@Composable
private fun MyWeightTab(uiState: WeightUiState) {
    val modelProducer = remember { ChartEntryModelProducer() }

    LaunchedEffect(uiState.weightHistory) {
        if (uiState.weightHistory.isNotEmpty()) {
            val entries = uiState.weightHistory
                .sortedBy { it.recordedAt }
                .mapIndexed { index, record ->
                    entryOf(index.toFloat(), record.weightG / 1000f)  // x = index
                }
            modelProducer.setEntries(entries)
        }
    }
    // 나머지 동일

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // 최근 체중 요약
        item {
            WeightSummaryCard(
                latestWeightG = uiState.latestWeightG,
                recordCount = uiState.weightHistory.size
            )
        }

        // 그래프
        item {
            if (uiState.weightHistory.size >= 2) {
                // xLabels: 기록 날짜 리스트
                val xLabels = uiState.weightHistory
                    .sortedBy { it.recordedAt }
                    .map { formatDate(it.recordedAt).substring(5) }
                WeightChartCard(modelProducer = modelProducer, isBreedChart = false,
                    xLabels =xLabels)
            } else {
                NoChartCard(message = "체중 기록이 2개 이상이면 그래프가 표시돼요")
            }
        }

        // 기록 리스트 헤더
        item {
            Text(
                text = "기록 목록",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MyCatColors.OnBackground,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 기록 없을 때
        if (uiState.weightHistory.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "아직 체중 기록이 없어요\nFAB 버튼을 눌러 추가해보세요",
                        fontSize = 13.sp,
                        color = MyCatColors.TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // 기록 리스트 (최신순)
        items(
            items = uiState.weightHistory.sortedByDescending { it.recordedAt },
            key = { it.id }
        ) { record ->
            WeightRecordItem(
                record = record,
                birthDate = uiState.birthDate  // 추가
            )
        }
    }
}

// ─── 4. BreedAverageTab ──────────────────────────────────────────────
@Composable
private fun BreedAverageTab(uiState: WeightUiState) {
    val modelProducer = remember { ChartEntryModelProducer() }

    LaunchedEffect(uiState.breedAverageData) {
        if (uiState.breedAverageData.isNotEmpty()) {
            val avgEntries = uiState.breedAverageData.mapIndexed { index, point ->
                entryOf(index.toFloat(), point.avgWeightG / 1000f)  // x = index
            }
            val minEntries = uiState.breedAverageData.mapIndexed { index, point ->
                entryOf(index.toFloat(), point.weightMinG / 1000f)
            }
            val maxEntries = uiState.breedAverageData.mapIndexed { index, point ->
                entryOf(index.toFloat(), point.weightMaxG / 1000f)
            }
            modelProducer.setEntries(listOf(avgEntries, minEntries, maxEntries))
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 범례
        BreedChartLegend()
        // xLabels: 기록 날짜 리스트
        val xLabels = uiState.breedAverageData.map { "${it.month}개월" }
        // 그래프
        if (uiState.breedAverageData.isNotEmpty()) {
            WeightChartCard(
                modelProducer = modelProducer,
                isBreedChart = true,
                xLabels = xLabels,
                modifier = Modifier.height(300.dp)
            )
        } else {
            NoChartCard(message = "품종 정보가 없으면 평균 데이터를 표시할 수 없어요")
        }

        // 현재 개월수 평균 체중 정보
        uiState.breedAverageData.firstOrNull()?.let {
            Text(
                text = "1개월 ~ ${uiState.breedAverageData.lastOrNull()?.month ?: 0}개월 데이터",
                fontSize = 12.sp,
                color = MyCatColors.TextMuted
            )
        }
    }
}

// ─── 5. 공통 컴포넌트 ────────────────────────────────────────────────
@Composable
private fun WeightSummaryCard(latestWeightG: Int?, recordCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MyCatColors.OnPrimary),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (latestWeightG != null)
                        "${latestWeightG / 1000.0}kg" else "-",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MyCatColors.Primary
                )
                Text(
                    text = "현재 체중",
                    fontSize = 12.sp,
                    color = MyCatColors.TextMuted
                )
            }
            VerticalDivider(
                modifier = Modifier.height(48.dp),
                color = MyCatColors.Border
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${recordCount}회",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MyCatColors.Primary
                )
                Text(
                    text = "총 기록",
                    fontSize = 12.sp,
                    color = MyCatColors.TextMuted
                )
            }
        }
    }
}



@Composable
private fun WeightChartCard(
    modelProducer: ChartEntryModelProducer,
    isBreedChart: Boolean = false,
    xLabels: List<String> = emptyList(),
    modifier: Modifier = Modifier.height(250.dp)
) {
    val lines = if (isBreedChart) {
        listOf(
            LineChart.LineSpec(lineColor = MyCatColors.Primary.hashCode()),
            LineChart.LineSpec(lineColor = MyCatColors.Success.hashCode()),
            LineChart.LineSpec(lineColor = MyCatColors.Secondary.hashCode())
        )
    } else {
        listOf(
            LineChart.LineSpec(lineColor = MyCatColors.Primary.hashCode())
        )
    }
    val labelComponent = textComponent(
        color = MyCatColors.OnBackground  // 강제로 다크 컬러 지정
    )
    // y축 포맷터
    val yAxisFormatter = AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
        "${String.format("%.1f", value)}kg"
    }

    // x축 포맷터
    val xAxisFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        xLabels.getOrNull(value.toInt()) ?: ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MyCatColors.OnPrimary),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Chart(
            chart = lineChart(lines = lines),
            chartModelProducer = modelProducer,
            startAxis = rememberStartAxis(
                label = labelComponent,
                valueFormatter = yAxisFormatter,
                itemPlacer = remember { AxisItemPlacer.Vertical.default(maxItemCount = 5) }
            ),
            bottomAxis = rememberBottomAxis(
                label = labelComponent,
                valueFormatter = xAxisFormatter,
                itemPlacer = remember {
                    AxisItemPlacer.Horizontal.default(
                        spacing = if (xLabels.size > 6) xLabels.size / 6 else 1,
                        addExtremeLabelPadding = true
                    )
                }
            ),
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
private fun NoChartCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MyCatColors.OnPrimary),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                fontSize = 13.sp,
                color = MyCatColors.TextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun WeightRecordItem(
    record: WeightRecord,
    birthDate: String  // "2024-09" 형식
) {
    val date = formatDate(record.recordedAt)
    val ageMonth = calculateAgeMonthAt(birthDate, record.recordedAt)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MyCatColors.OnPrimary)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "${record.weightG / 1000.0}kg",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MyCatColors.OnBackground
            )
            record.memo?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = MyCatColors.TextMuted
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = date,
                fontSize = 12.sp,
                color = MyCatColors.TextMuted
            )
            Text(
                text = "${ageMonth}개월",
                fontSize = 11.sp,
                color = MyCatColors.Primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 특정 시점의 개월수 계산
private fun calculateAgeMonthAt(birthDate: String, recordedAt: Long): Int {
    return try {
        val parts = birthDate.split("-")
        val birthYear = parts[0].toInt()
        val birthMonth = parts[1].toInt()
        val local = Instant.fromEpochMilliseconds(recordedAt)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        (local.year - birthYear) * 12 + (local.monthNumber - birthMonth)
    } catch (e: Exception) {
        0
    }
}

@Composable
private fun BreedChartLegend() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LegendItem(color = MyCatColors.Primary, label = "평균")
        LegendItem(color = MyCatColors.Success, label = "최소")
        LegendItem(color = MyCatColors.Secondary, label = "최대")
    }
}

@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(text = label, fontSize = 12.sp, color = MyCatColors.TextMuted)
    }
}

// ─── 6. WeightInputDialog ────────────────────────────────────────────
@Composable
private fun WeightInputDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var weightInput by remember { mutableStateOf("") }
    var memoInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "체중 기록",
                fontWeight = FontWeight.Bold,
                color = MyCatColors.OnBackground
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = {
                        weightInput = it
                        isError = false
                    },
                    label = { Text("체중 (kg)") },
                    placeholder = { Text("예: 3.5") },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("올바른 체중을 입력해주세요", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = memoInput,
                    onValueChange = { memoInput = it },
                    label = { Text("메모 (선택)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (weightInput.toDoubleOrNull() == null) {
                        isError = true
                    } else {
                        onSave(weightInput, memoInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MyCatColors.Primary
                )
            ) {
                Text("저장", color = MyCatColors.OnPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = MyCatColors.TextMuted)
            }
        },
        containerColor = MyCatColors.Background
    )
}

// ─── 7. 날짜 포맷 ────────────────────────────────────────────────────
private fun formatDate(millis: Long): String {
    val instant = Instant.fromEpochMilliseconds(millis)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.year}.${local.monthNumber.toString().padStart(2, '0')}.${local.dayOfMonth.toString().padStart(2, '0')}"
}

// ─── 8. Preview ──────────────────────────────────────────────────────
@Preview(showBackground = true, backgroundColor = 0xFFFFF8F3)
@Composable
private fun WeightContentPreview() {
    MyCatTheme {
        WeightContent(
            uiState = WeightUiState(
                catName = "별이",
                latestWeightG = 3200,
                birthDate = "2025-09",
                weightHistory = listOf(
                    WeightRecord(1L, 1L, 2800, System.currentTimeMillis() - 86400000 * 30),
                    WeightRecord(2L, 1L, 3000, System.currentTimeMillis() - 86400000 * 20),
                    WeightRecord(3L, 1L, 3200, System.currentTimeMillis() - 86400000 * 10),
                ),
                breedAverageData = listOf(
                    BreedAvgPoint(1, 300, 500, 400),
                    BreedAvgPoint(3, 800, 1200, 1000),
                    BreedAvgPoint(6, 1500, 2000, 1750),
                    BreedAvgPoint(12, 2500, 3500, 3000),
                    BreedAvgPoint(24, 3000, 4500, 3750),
                )
            ),
            onBack = {},
            onTabSelected = {},
            onFabClick = {},
            onDialogDismiss = {},
            onWeightSave = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F3)
@Composable
private fun WeightInputDialogPreview() {
    MyCatTheme {
        WeightInputDialog(
            onDismiss = {},
            onSave = { _, _ -> }
        )
    }
}