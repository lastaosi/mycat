package com.lastaosi.mycat.presentation.vaccination

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lastaosi.mycat.domain.model.VaccinationRecord
import com.lastaosi.mycat.ui.theme.MyCatColors
import com.lastaosi.mycat.ui.theme.MyCatTheme
import com.lastaosi.mycat.util.DateVisualTransformation
import com.lastaosi.mycat.util.L
import kotlinx.datetime.Instant
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

// ─── 1. Screen ───────────────────────────────────────────────────────
@Composable
fun VaccinationScreen(
    catId: Long,
    onBack: () -> Unit,
    viewModel: VaccinationViewModel = koinViewModel()
) {
    LaunchedEffect(catId) {
        viewModel.loadData(catId)
    }
    val uiState by viewModel.uiState.collectAsState()
    VaccinationContent(
        uiState = uiState,
        onBack = onBack,
        onFabClick = viewModel::onFabClick,
        onEditClick = viewModel::onEditClick,
        onDelete = viewModel::onDelete,
        onDialogDismiss = viewModel::onDialogDismiss,
        onSave = viewModel::onSave
    )
}

// ─── 2. Content ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationContent(
    uiState: VaccinationUiState,
    onBack: () -> Unit,
    onFabClick: () -> Unit,
    onEditClick: (VaccinationRecord) -> Unit,
    onDelete: (Long) -> Unit,
    onDialogDismiss: () -> Unit,
    onSave: (String, Long, Long?, String, Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "예방접종",
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onFabClick,
                containerColor = MyCatColors.Primary,
                contentColor = MyCatColors.OnPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "접종 추가")
            }
        },
        containerColor = MyCatColors.Background
    ) { innerPadding ->
        if (uiState.records.isEmpty()) {
            // 빈 상태
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "💉", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "접종 기록이 없어요",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MyCatColors.OnBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "FAB 버튼을 눌러 추가해보세요",
                        fontSize = 13.sp,
                        color = MyCatColors.TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(
                    items = uiState.records,
                    key = { it.id }
                ) { record ->
                    VaccinationRecordItem(
                        record = record,
                        onEditClick = { onEditClick(record) },
                        onDeleteClick = { onDelete(record.id) }
                    )
                }
            }
        }
    }

    // 입력/수정 다이얼로그
    if (uiState.showInputDialog) {
        VaccinationInputDialog(
            editingRecord = uiState.editingRecord,
            onDismiss = onDialogDismiss,
            onSave = onSave
        )
    }
}

// ─── 3. VaccinationRecordItem ────────────────────────────────────────
@Composable
private fun VaccinationRecordItem(
    record: VaccinationRecord,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MyCatColors.OnPrimary),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 접종명 + 알림 여부
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "💉", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MyCatColors.OnBackground
                    )
                    if (record.isNotificationEnabled) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MyCatColors.Primary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "알림",
                                fontSize = 10.sp,
                                color = MyCatColors.Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 수정/삭제 버튼
                Row {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "수정",
                            tint = MyCatColors.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "삭제",
                            tint = MyCatColors.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MyCatColors.Border, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // 접종일
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "접종일",
                        fontSize = 11.sp,
                        color = MyCatColors.TextMuted
                    )
                    Text(
                        text = formatDate(record.vaccinatedAt),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MyCatColors.OnBackground
                    )
                }

                // 다음 예정일
                record.nextDueAt?.let { nextDue ->
                    Column {
                        Text(
                            text = "다음 예정일",
                            fontSize = 11.sp,
                            color = MyCatColors.TextMuted
                        )
                        val dDay = calculateDDay(nextDue)
                        Text(
                            text = "${formatDate(nextDue)} (${formatDDay(dDay)})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dDay in 0..7) MyCatColors.Primary
                            else MyCatColors.OnBackground
                        )
                    }
                }
            }

            // 메모
            record.memo?.let { memo ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = memo,
                    fontSize = 12.sp,
                    color = MyCatColors.TextMuted
                )
            }
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("접종 기록 삭제") },
            text = { Text("${record.title} 기록을 삭제할까요?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick()
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소", color = MyCatColors.TextMuted)
                }
            },
            containerColor = MyCatColors.Background
        )
    }
}

// ─── 4. VaccinationInputDialog ───────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaccinationInputDialog(
    editingRecord: VaccinationRecord?,
    onDismiss: () -> Unit,
    onSave: (String, Long, Long?, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(editingRecord?.title ?: "") }
    // editingRecord 초기값 수정
    var vaccinatedAt by remember { mutableStateOf(
        editingRecord?.let { formatDateToDigits(it.vaccinatedAt) } ?: ""
    )}
    var nextDueAt by remember { mutableStateOf(
        editingRecord?.nextDueAt?.let { formatDateToDigits(it) } ?: ""
    )}
    var memo by remember { mutableStateOf(editingRecord?.memo ?: "") }
    var isNotificationEnabled by remember { mutableStateOf(
        editingRecord?.isNotificationEnabled ?: true
    )}
    var isTitleError by remember { mutableStateOf(false) }
    var isDateError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingRecord != null) "접종 기록 수정" else "접종 기록 추가",
                fontWeight = FontWeight.Bold,
                color = MyCatColors.OnBackground
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // 접종명
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; isTitleError = false },
                    label = { Text("접종명 *") },
                    isError = isTitleError,
                    supportingText = if (isTitleError) {
                        { Text("접종명을 입력해주세요") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // 접종일
                OutlinedTextField(
                    value = vaccinatedAt,
                    onValueChange = {
                        // 숫자만 허용, 최대 8자리
                        val digits = it.filter { c -> c.isDigit() }.take(8)
                        vaccinatedAt = digits
                        isDateError = false
                    },
                    label = { Text("접종일 *") },
                    placeholder = { Text("20260401") },
                    isError = isDateError,
                    supportingText = if (isDateError) {
                        { Text("날짜 형식을 확인해주세요") }
                    } else null,
                    visualTransformation = com.lastaosi.mycat.util.DateVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

// 다음 예정일
                OutlinedTextField(
                    value = nextDueAt,
                    onValueChange = {
                        val digits = it.filter { c -> c.isDigit() }.take(8)
                        nextDueAt = digits
                    },
                    label = { Text("다음 예정일 (선택)") },
                    placeholder = { Text("20260701") },
                    visualTransformation = DateVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 메모
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("메모 (선택)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 알림 여부
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "다음 예정일 알림",
                        fontSize = 14.sp,
                        color = MyCatColors.OnBackground
                    )
                    Switch(
                        checked = isNotificationEnabled,
                        onCheckedChange = { isNotificationEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MyCatColors.OnPrimary,
                            checkedTrackColor = MyCatColors.Primary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // 유효성 검사
                    if (title.isBlank()) { isTitleError = true; return@Button }
                    val vaccinatedMillis = parseDate(vaccinatedAt)
                    if (vaccinatedMillis == null) { isDateError = true; return@Button }
                    val nextDueMillis = if (nextDueAt.isBlank()) null else parseDate(nextDueAt)
                    onSave(title, vaccinatedMillis, nextDueMillis, memo, isNotificationEnabled)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MyCatColors.Primary)
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

// ─── 5. 날짜 유틸 ────────────────────────────────────────────────────
private fun formatDate(millis: Long): String {
    val local = Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.year}-${local.monthNumber.toString().padStart(2,'0')}-${local.dayOfMonth.toString().padStart(2,'0')}"
}

private fun parseDate(dateStr: String): Long? {
    return try {
        // 숫자만 있는 경우 (20260401) → 2026-04-01 변환
        val cleaned = dateStr.filter { it.isDigit() }
        if (cleaned.length != 8) return null
        val year = cleaned.substring(0, 4).toInt()
        val month = cleaned.substring(4, 6).toInt()
        val day = cleaned.substring(6, 8).toInt()
        val date = LocalDate(year, month, day)
        date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalTime::class)
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

// 숫자 8자리 포맷 함수 추가
private fun formatDateToDigits(millis: Long): String {
    val local = Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.year}${local.monthNumber.toString().padStart(2,'0')}${local.dayOfMonth.toString().padStart(2,'0')}"
}

// ─── 6. Preview ──────────────────────────────────────────────────────
@Preview(showBackground = true, backgroundColor = 0xFFFFF8F3)
@Composable
private fun VaccinationContentPreview() {
    MyCatTheme {
        VaccinationContent(
            uiState = VaccinationUiState(
                catName = "별이",
                records = listOf(
                    VaccinationRecord(
                        id = 1L,
                        catId = 1L,
                        title = "3차 종합백신",
                        vaccinatedAt = System.currentTimeMillis() - 86400000L * 30,
                        nextDueAt = System.currentTimeMillis() + 86400000L * 3,
                        memo = "이상 반응 없음",
                        isNotificationEnabled = true
                    ),
                    VaccinationRecord(
                        id = 2L,
                        catId = 1L,
                        title = "광견병 예방접종",
                        vaccinatedAt = System.currentTimeMillis() - 86400000L * 60,
                        nextDueAt = null,
                        isNotificationEnabled = false
                    )
                )
            ),
            onBack = {},
            onFabClick = {},
            onEditClick = {},
            onDelete = {},
            onDialogDismiss = {},
            onSave = { _, _, _, _, _ -> }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F3)
@Composable
private fun VaccinationInputDialogPreview() {
    MyCatTheme {
        VaccinationInputDialog(
            editingRecord = null,
            onDismiss = {},
            onSave = { _, _, _, _, _ -> }
        )
    }
}