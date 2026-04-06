package com.lastaosi.mycat.presentation.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
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
import com.lastaosi.mycat.domain.model.Medication
import com.lastaosi.mycat.domain.model.MedicationType
import com.lastaosi.mycat.presentation.weight.WeightAction
import com.lastaosi.mycat.ui.theme.MyCatColors
import com.lastaosi.mycat.ui.theme.MyCatTheme
import com.lastaosi.mycat.util.DatePickerField
import com.lastaosi.mycat.util.DateVisualTransformation
import com.lastaosi.mycat.util.L
import com.lastaosi.mycat.util.TimeVisualTransformation
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

// ─── 1. Screen ───────────────────────────────────────────────────────
@Composable
fun MedicationScreen(
    catId: Long,
    onBack: () -> Unit,
    viewModel: MedicationViewModel = koinViewModel()
) {
    LaunchedEffect(catId) {
        viewModel.loadData(catId)
    }
    val uiState by viewModel.uiState.collectAsState()
    MedicationContent(
        uiState = uiState,
        onBack = onBack,
        onAction = viewModel::onAction
    )
}

// ─── 2. Content ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationContent(
    uiState: MedicationUiState,
    onBack: () -> Unit,
    onAction: (MedicationAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "약 복용 관리",
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
                onClick = { onAction(MedicationAction.FabClick)},
                containerColor = MyCatColors.Primary,
                contentColor = MyCatColors.OnPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "약 추가")
            }
        },
        containerColor = MyCatColors.Background
    ) { innerPadding ->
        if (uiState.activeMedications.isEmpty() && uiState.inactiveMedications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "💊", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "약 복용 기록이 없어요",
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
                // 복용 중
                if (uiState.activeMedications.isNotEmpty()) {
                    item {
                        SectionHeader(title = "복용 중", emoji = "💊")
                    }
                    items(
                        items = uiState.activeMedications,
                        key = { it.id }
                    ) { medication ->
                        MedicationItem(
                            medication = medication,
                            onEditClick = {
                                onAction(MedicationAction.EditClick(medication))},
                            onDeleteClick = {
                                onAction(MedicationAction.DeleteClick(medication.id))
                                            },
                            onToggleActive = {
                                onAction(MedicationAction.ToggleActive(medication))
                                }
                        )
                    }
                }

                // 완료된 약
                if (uiState.inactiveMedications.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        SectionHeader(title = "완료", emoji = "✅")
                    }
                    items(
                        items = uiState.inactiveMedications,
                        key = { it.id }
                    ) { medication ->
                        MedicationItem(
                            medication = medication,
                            onEditClick = {
                                onAction(MedicationAction.EditClick(medication))},
                            onDeleteClick = {
                                onAction(MedicationAction.DeleteClick(medication.id))
                            },
                            onToggleActive = {
                                onAction(MedicationAction.ToggleActive(medication))
                            }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showInputDialog) {
        MedicationInputDialog(
            editingMedication = uiState.editingMedication,
            onDismiss = {onAction(MedicationAction.DialogDismiss)},
            onSave = { name, selectedType, dosage, startMillis, endMillis, interval, memo, alarmTimes ->
                onAction(
                    MedicationAction.MedicationSave(
                        name = name,
                        medicationType = selectedType,
                        dosage = dosage,
                        startDate = startMillis,
                        endDate = endMillis,
                        intervalDays = interval,
                        memo = memo,
                        alarmTimes = alarmTimes
                    )
                )
            }
        )
    }
}

// ─── 3. SectionHeader ────────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String, emoji: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = emoji, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MyCatColors.OnBackground
        )
    }
}

// ─── 4. MedicationItem ───────────────────────────────────────────────
@Composable
private fun MedicationItem(
    medication: Medication,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleActive: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (medication.isActive) MyCatColors.OnPrimary
            else MyCatColors.OnPrimary.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 약 이름 + 타입 뱃지
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = medication.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MyCatColors.OnBackground
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    MedicationTypeBadge(type = medication.medicationType)
                }

                // 수정/삭제
                Row {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
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

            // 투약량
            medication.dosage?.let {
                Text(
                    text = "투약량: $it",
                    fontSize = 12.sp,
                    color = MyCatColors.TextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 날짜 정보
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text(text = "시작일", fontSize = 11.sp, color = MyCatColors.TextMuted)
                    Text(
                        text = formatDate(medication.startDate),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MyCatColors.OnBackground
                    )
                }
                medication.endDate?.let { endDate ->
                    Column {
                        Text(text = "종료일", fontSize = 11.sp, color = MyCatColors.TextMuted)
                        Text(
                            text = formatDate(endDate),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MyCatColors.OnBackground
                        )
                    }
                }
                medication.intervalDays?.let { interval ->
                    Column {
                        Text(text = "복용 간격", fontSize = 11.sp, color = MyCatColors.TextMuted)
                        Text(
                            text = "${interval}일마다",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MyCatColors.OnBackground
                        )
                    }
                }
            }

            // 메모
            medication.memo?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = it, fontSize = 12.sp, color = MyCatColors.TextMuted)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 복용 중 / 완료 토글 버튼
            OutlinedButton(
                onClick = onToggleActive,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (medication.isActive) MyCatColors.TextMuted
                    else MyCatColors.Primary
                )
            ) {
                Text(
                    text = if (medication.isActive) "복용 완료로 변경" else "복용 중으로 변경",
                    fontSize = 13.sp
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("약 삭제") },
            text = { Text("${medication.name} 을 삭제할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteClick()
                }) {
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

// ─── 5. MedicationTypeBadge ──────────────────────────────────────────
@Composable
private fun MedicationTypeBadge(type: MedicationType) {
    val (label, color) = when (type) {
        MedicationType.ONCE     -> "1회" to MyCatColors.TextMuted
        MedicationType.DAILY    -> "매일" to MyCatColors.Primary
        MedicationType.INTERVAL -> "간격" to MyCatColors.Secondary
        MedicationType.PERIOD   -> "기간" to MyCatColors.Success
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text = label, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

// ─── 6. MedicationInputDialog ────────────────────────────────────────
@Composable
private fun MedicationInputDialog(
    editingMedication: Medication?,
    onDismiss: () -> Unit,
    onSave: (String, MedicationType, String, Long, Long?, Int?, String, List<String>) -> Unit
) {

    // 오늘 날짜를 숫자 8자리로
    val todayDigits = run {
        val cal = java.util.Calendar.getInstance()
        val year = cal.get(java.util.Calendar.YEAR)
        val month = (cal.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        "$year$month$day"
    }

// 현재 시간을 4자리로
    val currentTimeDigits = run {
        val cal = java.util.Calendar.getInstance()
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val minute = cal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
        "$hour$minute"
    }
    var name by remember { mutableStateOf(editingMedication?.name ?: "") }
    var selectedType by remember { mutableStateOf(
        editingMedication?.medicationType ?: MedicationType.DAILY
    )}
    var dosage by remember { mutableStateOf(editingMedication?.dosage ?: "") }
    var alarmInput by remember { mutableStateOf(currentTimeDigits) }
    var startDate by remember { mutableStateOf<Long?>(editingMedication?.startDate) }
    var endDate by remember { mutableStateOf<Long?>(editingMedication?.endDate) }
    var intervalDays by remember { mutableStateOf(
        editingMedication?.intervalDays?.toString() ?: ""
    )}
    var memo by remember { mutableStateOf(editingMedication?.memo ?: "") }
    var isNameError by remember { mutableStateOf(false) }
    var isStartDateError by remember { mutableStateOf(false) }

    var alarmTimes by remember { mutableStateOf(listOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingMedication != null) "약 수정" else "약 추가",
                fontWeight = FontWeight.Bold,
                color = MyCatColors.OnBackground
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 약 이름
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; isNameError = false },
                    label = { Text("약 이름 *") },
                    isError = isNameError,
                    supportingText = if (isNameError) {
                        { Text("약 이름을 입력해주세요") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 복용 타입 선택
                Text(
                    text = "복용 타입",
                    fontSize = 13.sp,
                    color = MyCatColors.TextMuted
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MedicationType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = {
                                Text(
                                    text = when (type) {
                                        MedicationType.ONCE     -> "1회"
                                        MedicationType.DAILY    -> "매일"
                                        MedicationType.INTERVAL -> "간격"
                                        MedicationType.PERIOD   -> "기간"
                                    },
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }

                // 투약량
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("투약량 (선택, 예: 0.5ml)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 시작일
                DatePickerField(
                    value = startDate,
                    onDateSelected = {
                        startDate = it
                        isStartDateError = false
                    },
                    label = "시작일 *",
                    isError = isStartDateError,
                    errorMessage = "시작일을 선택해주세요"
                )

// 종료일 (ONCE 제외)
                if (selectedType != MedicationType.ONCE) {
                    DatePickerField(
                        value = endDate,
                        onDateSelected = { endDate = it },
                        label = "종료일 (선택)"
                    )
                }

                // 복용 간격 (INTERVAL만)
                if (selectedType == MedicationType.INTERVAL) {
                    OutlinedTextField(
                        value = intervalDays,
                        onValueChange = { intervalDays = it.filter { c -> c.isDigit() } },
                        label = { Text("복용 간격 (일)") },
                        placeholder = { Text("예: 3") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 메모
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("메모 (선택)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (selectedType != MedicationType.ONCE) {
                    Text(
                        text = "복용 알림 시간",
                        fontSize = 13.sp,
                        color = MyCatColors.TextMuted
                    )

                    alarmTimes.forEach { time ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = time, fontSize = 14.sp, color = MyCatColors.OnBackground)
                            IconButton(
                                onClick = { alarmTimes = alarmTimes - time },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "삭제",
                                    tint = MyCatColors.TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = alarmInput,
                            onValueChange = {
                                alarmInput = it.filter { c -> c.isDigit() }.take(4)
                            },
                            label = { Text("시간 (예: 0800 → 08:00)") },
                            visualTransformation = TimeVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (alarmInput.length == 4) {
                                    val formatted = "${alarmInput.substring(0,2)}:${alarmInput.substring(2,4)}"
                                    if (formatted !in alarmTimes) {
                                        alarmTimes = alarmTimes + formatted
                                    }
                                    alarmInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MyCatColors.Primary)
                        ) {
                            Text("추가")
                        }
                    }
                }
            }


        },
        confirmButton = {
            Button(
                onClick = {
                    L.d( "저장 시 alarmTimes: $alarmTimes")
                    if (name.isBlank()) { isNameError = true; return@Button }
                    if (startDate == null) { isStartDateError = true; return@Button }

                    val endMillis = endDate
                    val interval = if (selectedType == MedicationType.INTERVAL)
                        intervalDays.toIntOrNull() else null
                    onSave(name, selectedType, dosage, startDate!!, endMillis, interval, memo,alarmTimes)
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

    // ONCE 제외하고 알람 설정 가능

}

// ─── 7. 날짜 유틸 ────────────────────────────────────────────────────
private fun formatDate(millis: Long): String {
    val local = Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.year}-${local.monthNumber.toString().padStart(2,'0')}-${local.dayOfMonth.toString().padStart(2,'0')}"
}

private fun formatDateToDigits(millis: Long): String {
    val local = Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.year}${local.monthNumber.toString().padStart(2,'0')}${local.dayOfMonth.toString().padStart(2,'0')}"
}

private fun parseDate(dateStr: String): Long? {
    return try {
        val cleaned = dateStr.filter { it.isDigit() }
        if (cleaned.length != 8) return null
        val date = LocalDate(
            cleaned.substring(0, 4).toInt(),
            cleaned.substring(4, 6).toInt(),
            cleaned.substring(6, 8).toInt()
        )
        date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    } catch (e: Exception) {
        null
    }
}

// ─── 8. Preview ──────────────────────────────────────────────────────
@Preview(showBackground = true, backgroundColor = 0xFFFFF8F3)
@Composable
private fun MedicationContentPreview() {
    MyCatTheme {
        MedicationContent(
            uiState = MedicationUiState(
                catName = "별이",
                activeMedications = listOf(
                    Medication(
                        id = 1L, catId = 1L,
                        name = "항생제",
                        medicationType = MedicationType.DAILY,
                        dosage = "0.5ml",
                        startDate = System.currentTimeMillis() - 86400000L * 3,
                        endDate = System.currentTimeMillis() + 86400000L * 4,
                        isActive = true,
                        createdAt = System.currentTimeMillis()
                    )
                ),
                inactiveMedications = listOf(
                    Medication(
                        id = 2L, catId = 1L,
                        name = "구충제",
                        medicationType = MedicationType.ONCE,
                        startDate = System.currentTimeMillis() - 86400000L * 30,
                        isActive = false,
                        createdAt = System.currentTimeMillis()
                    )
                )
            ),
            onBack = {},
            onAction = {}
        )
    }
}