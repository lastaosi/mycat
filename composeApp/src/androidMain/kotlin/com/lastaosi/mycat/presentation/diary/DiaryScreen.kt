package com.lastaosi.mycat.presentation.diary

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.lastaosi.mycat.domain.model.CatDiary
import com.lastaosi.mycat.domain.model.DiaryMood
import com.lastaosi.mycat.ui.theme.MyCatColors
import com.lastaosi.mycat.ui.theme.MyCatTheme
import com.lastaosi.mycat.util.saveImageToInternalStorage
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// ─── 1. Screen ───────────────────────────────────────────────────────
@Composable
fun DiaryScreen(
    catId: Long,
    onBack: () -> Unit,
    viewModel: DiaryViewModel = koinViewModel()
) {
    LaunchedEffect(catId) {
        viewModel.loadData(catId)
    }
    val uiState by viewModel.uiState.collectAsState()
    DiaryContent(
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
fun DiaryContent(
    uiState: DiaryUiState,
    onBack: () -> Unit,
    onFabClick: () -> Unit,
    onEditClick: (CatDiary) -> Unit,
    onDelete: (Long) -> Unit,
    onDialogDismiss: () -> Unit,
    onSave: (String, String, DiaryMood?, String?, Long) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "다이어리",
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
                Icon(Icons.Default.Add, contentDescription = "다이어리 추가")
            }
        },
        containerColor = MyCatColors.Background
    ) { innerPadding ->
        if (uiState.diaries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "📝", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "아직 다이어리가 없어요",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MyCatColors.OnBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "별이와의 소중한 순간을 기록해보세요",
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
                    items = uiState.diaries,
                    key = { it.id }
                ) { diary ->
                    DiaryItem(
                        diary = diary,
                        onEditClick = { onEditClick(diary) },
                        onDeleteClick = { onDelete(diary.id) }
                    )
                }
            }
        }
    }

    if (uiState.showInputDialog) {
        DiaryInputDialog(
            editingDiary = uiState.editingDiary,
            onDismiss = onDialogDismiss,
            onSave = onSave
        )
    }
}

// ─── 3. DiaryItem ────────────────────────────────────────────────────
@Composable
private fun DiaryItem(
    diary: CatDiary,
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
            // 헤더: 날짜 + 기분 + 수정/삭제
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 기분 이모지
                    diary.mood?.let {
                        Text(text = it.toEmoji(), fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    // 날짜
                    Text(
                        text = formatDate(diary.createdAt),
                        fontSize = 12.sp,
                        color = MyCatColors.TextMuted
                    )
                }
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

            Spacer(modifier = Modifier.height(6.dp))

            // 제목
            diary.title?.let { title ->
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MyCatColors.OnBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 내용
            Text(
                text = diary.content,
                fontSize = 13.sp,
                color = MyCatColors.OnBackground.copy(alpha = 0.8f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // 사진
            diary.photoPath?.let { path ->
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = path,
                    contentDescription = "다이어리 사진",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("다이어리 삭제") },
            text = { Text("이 기록을 삭제할까요?") },
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

// ─── 4. DiaryInputDialog ─────────────────────────────────────────────
@OptIn(ExperimentalTime::class)
@Composable
private fun DiaryInputDialog(
    editingDiary: CatDiary?,
    onDismiss: () -> Unit,
    onSave: (String, String, DiaryMood?, String?, Long) -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(editingDiary?.title ?: "") }
    var content by remember { mutableStateOf(editingDiary?.content ?: "") }
    var selectedMood by remember { mutableStateOf(editingDiary?.mood) }
    var photoPath by remember { mutableStateOf(editingDiary?.photoPath) }
    var dateInput by remember { mutableStateOf(
        editingDiary?.let { formatDateToDigits(it.createdAt) }
            ?: formatDateToDigits(Clock.System.now().toEpochMilliseconds())
    )}
    var isContentError by remember { mutableStateOf(false) }
    var isDateError by remember { mutableStateOf(false) }

    // 갤러리 런처
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val saved = saveImageToInternalStorage(context, it)
            photoPath = saved
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingDiary != null) "다이어리 수정" else "다이어리 작성",
                fontWeight = FontWeight.Bold,
                color = MyCatColors.OnBackground
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 날짜 선택
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = {
                        dateInput = it.filter { c -> c.isDigit() }.take(8)
                        isDateError = false
                    },
                    label = { Text("날짜 *") },
                    placeholder = { Text("20260401") },
                    isError = isDateError,
                    supportingText = if (isDateError) {
                        { Text("날짜 형식을 확인해주세요") }
                    } else null,
                    visualTransformation = com.lastaosi.mycat.util.DateVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 제목 (선택)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("제목 (선택)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 내용 (필수)
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it; isContentError = false },
                    label = { Text("내용 *") },
                    isError = isContentError,
                    supportingText = if (isContentError) {
                        { Text("내용을 입력해주세요") }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )

                // 기분 선택
                Text(
                    text = "기분",
                    fontSize = 13.sp,
                    color = MyCatColors.TextMuted
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DiaryMood.entries.forEach { mood ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedMood == mood) MyCatColors.Surface
                                    else MyCatColors.Background
                                )
                                .border(
                                    width = if (selectedMood == mood) 2.dp else 1.dp,
                                    color = if (selectedMood == mood) MyCatColors.Primary
                                    else MyCatColors.Border,
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedMood = if (selectedMood == mood) null else mood
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = mood.toEmoji(), fontSize = 20.sp)
                        }
                    }
                }

                // 사진 선택
                Text(
                    text = "사진",
                    fontSize = 13.sp,
                    color = MyCatColors.TextMuted
                )
                if (photoPath != null) {
                    Box {
                        AsyncImage(
                            model = photoPath,
                            contentDescription = "선택된 사진",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { galleryLauncher.launch("image/*") }
                        )
                        // 사진 제거 버튼
                        TextButton(
                            onClick = { photoPath = null },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text("제거", color = MyCatColors.OnPrimary, fontSize = 12.sp)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "🖼️  사진 선택",
                            color = MyCatColors.TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isBlank()) { isContentError = true; return@Button }
                    val dateMillis = parseDate(dateInput)
                    if (dateMillis == null) { isDateError = true; return@Button }
                    onSave(title, content, selectedMood, photoPath, dateMillis)
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

// ─── 5. DiaryMood 확장함수 ───────────────────────────────────────────
fun DiaryMood.toEmoji(): String = when (this) {
    DiaryMood.HAPPY   -> "😸"
    DiaryMood.NORMAL  -> "😐"
    DiaryMood.SAD     -> "😿"
    DiaryMood.SICK    -> "🤒"
    DiaryMood.PLAYFUL -> "😺"
}

// ─── 6. 날짜 유틸 ────────────────────────────────────────────────────
private fun formatDate(millis: Long): String {
    val local = Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.year}.${local.monthNumber.toString().padStart(2,'0')}.${local.dayOfMonth.toString().padStart(2,'0')}"
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

// ─── 7. Preview ──────────────────────────────────────────────────────
@Preview(showBackground = true, backgroundColor = 0xFFFFF8F3)
@Composable
private fun DiaryContentPreview() {
    MyCatTheme {
        DiaryContent(
            uiState = DiaryUiState(
                catName = "별이",
                diaries = listOf(
                    CatDiary(
                        id = 1L, catId = 1L,
                        title = "오늘의 별이",
                        content = "오늘 별이가 새벽에 기세차게 장난감을 가져와서 같이 놀았다. 너무 귀여웠다.",
                        mood = DiaryMood.HAPPY,
                        createdAt = System.currentTimeMillis() - 86400000L,
                        updatedAt = System.currentTimeMillis()
                    ),
                    CatDiary(
                        id = 2L, catId = 1L,
                        title = null,
                        content = "병원 다녀왔다. 건강하다고 해서 다행이었다.",
                        mood = DiaryMood.NORMAL,
                        createdAt = System.currentTimeMillis() - 86400000L * 3,
                        updatedAt = System.currentTimeMillis()
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