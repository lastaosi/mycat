package com.lastaosi.mycat.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.lastaosi.mycat.domain.model.Breed
import com.lastaosi.mycat.domain.model.Gender
import com.lastaosi.mycat.util.BirthDateVisualTransformation
import org.koin.compose.viewmodel.koinViewModel
import java.io.File

private val BrownPrimary = Color(0xFF8B5E3C)
private val BeigeBackground = Color(0xFFFFF8F0)

@Composable
fun ProfileRegisterScreen(
    onSaved: () -> Unit,
    viewModel: ProfileRegisterViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ProfileRegisterContent(
        uiState = uiState,
        onSaved = onSaved,
        onPhotoSelected = viewModel::onPhotoSelected,
        onRecognizeBreed = viewModel::recognizeBreed,
        onNameChanged = viewModel::onNameChanged,
        onBirthDateChanged = viewModel::onBirthDateChanged,
        onGenderSelected = viewModel::onGenderSelected,
        onBreedNameCustomChanged = viewModel::onBreedNameCustomChanged,
        onWeightChanged = viewModel::onWeightChanged,
        onHeightChanged = viewModel::onHeightChanged,
        onNeuteredChanged = viewModel::onNeuteredChanged,
        onMemoChanged = viewModel::onMemoChanged,
        onSave = viewModel::save,
        onErrorShown = viewModel::clearError,
        onBreedSearchQueryChanged = viewModel::onBreedSearchQueryChanged,
        onBreedSelected = viewModel::onBreedSelected,
    )
}

@Composable
fun ProfileRegisterContent(
    uiState: ProfileRegisterUiState,
    onSaved: () -> Unit,
    onPhotoSelected: (String) -> Unit,
    onRecognizeBreed: (ByteArray) -> Unit,
    onNameChanged: (String) -> Unit,
    onBirthDateChanged: (String) -> Unit,
    onGenderSelected: (Gender) -> Unit,
    onBreedNameCustomChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onHeightChanged: (String) -> Unit,
    onNeuteredChanged: (Boolean) -> Unit,
    onMemoChanged: (String) -> Unit,
    onSave: () -> Unit,
    onErrorShown: () -> Unit = {},
    onBreedSearchQueryChanged: (String) -> Unit,
    onBreedSelected: (Breed) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showPhotoDialog by remember { mutableStateOf(false) }

    // 카메라 촬영용 임시 URI
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // 갤러리 런처
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.toString()?.let { onPhotoSelected(it) }
    }

    // 카메라 런처
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.toString()?.let { onPhotoSelected(it) }
        }
    }

    // 카메라 권한 런처
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val imageFile = File(
                context.externalCacheDir,
                "cat_photo_${System.currentTimeMillis()}.jpg"
            )
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    // 저장 완료
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    // 에러 메시지
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    // 사진 선택 다이얼로그
    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { Text("사진 선택") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showPhotoDialog = false
                            cameraPermissionLauncher.launch(
                                android.Manifest.permission.CAMERA
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📷 카메라로 촬영", fontSize = 16.sp)
                    }
                    TextButton(
                        onClick = {
                            showPhotoDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🖼️ 갤러리에서 선택", fontSize = 16.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BeigeBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "고양이 등록",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = BrownPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 사진 등록 — 클릭 시 다이얼로그
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEEDDCC))
                    .border(2.dp, BrownPrimary, CircleShape)
                    .clickable { showPhotoDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (uiState.photoPath != null) {
                    AsyncImage(
                        model = uiState.photoPath,
                        contentDescription = "고양이 사진",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "사진 등록",
                        color = BrownPrimary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 품종 확인 버튼
            Button(
                onClick = {
                    uiState.photoPath?.let { path ->
                        val uri = android.net.Uri.parse(path)
                        val imageBytes = context.contentResolver
                            .openInputStream(uri)
                            ?.use { stream ->
                                // 비트맵으로 로드 후 리사이즈
                                val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                                val resized = android.graphics.Bitmap.createScaledBitmap(
                                    bitmap,
                                    800,
                                    (800f * bitmap.height / bitmap.width).toInt(),
                                    true
                                )
                                val outputStream = java.io.ByteArrayOutputStream()
                                resized.compress(
                                    android.graphics.Bitmap.CompressFormat.JPEG,
                                    80,
                                    outputStream
                                )
                                outputStream.toByteArray()
                            }
                        imageBytes?.let {  onRecognizeBreed(it) }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrownPrimary),
                enabled = uiState.photoPath != null,
                shape = RoundedCornerShape(8.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("🔍 품종 자동 인식")
                }
            }

            if (uiState.geminiBreedRaw != null) {
                Text(
                    text = "인식 결과: ${uiState.geminiBreedRaw} (${
                        String.format("%.0f", (uiState.geminiConfidence ?: 0.0) * 100)
                    }%)",
                    color = BrownPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 이름
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChanged,
                label = {  Text("이름 *", color = if (uiState.name.isBlank()) Color.Red else Color.Gray)},
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 생년월
            OutlinedTextField(
                value = uiState.birthDate.filter { it.isDigit() },
                onValueChange = onBirthDateChanged,
                label = { Text("생년월 *", color = if (uiState.birthDate.isBlank()) Color.Red else Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = BirthDateVisualTransformation(),
                placeholder = { Text("202303") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 성별
            Text(
                text = "성별",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Start)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.Start)
            ) {
                Gender.entries.forEach { gender ->
                    FilterChip(
                        selected = uiState.gender == gender,
                        onClick = { onGenderSelected(gender) },
                        label = {
                            Text(
                                when (gender) {
                                    Gender.MALE -> "수컷"
                                    Gender.FEMALE -> "암컷"
                                    Gender.UNKNOWN -> "모름"
                                }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 품종 직접 입력
            // 품종 검색/선택
            Text(
                text = "품종",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Start)
            )


/// 검색 결과 — 입력창 위에 표시
            if (uiState.breedSearchResults.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    uiState.breedSearchResults.forEach { breed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBreedSelected(breed) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = breed.nameKo,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = breed.nameEn,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                text = breed.sizeCategory,
                                fontSize = 12.sp,
                                color = BrownPrimary
                            )
                        }
                        HorizontalDivider()
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

// 검색 입력창 — 결과 아래에
            OutlinedTextField(
                value = uiState.breedSearchQuery,
                onValueChange = onBreedSearchQueryChanged,
                label = { Text("* 품종 검색 (예: 코리안, 페르시안)", color = if(uiState.breedSearchQuery.isBlank()) Color.Red else Color.Gray)},
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (uiState.breedSearchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            onBreedSearchQueryChanged("")
                            onBreedNameCustomChanged("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "초기화")
                        }
                    }
                }
            )

// Gemini 인식 결과랑 다를 때 직접 입력 옵션
            if (uiState.breedId == null && uiState.breedSearchQuery.isNotEmpty()
                && uiState.breedSearchResults.isEmpty()) {
                Text(
                    text = "검색 결과가 없으면 직접 입력된 이름으로 저장돼요",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 체중
            OutlinedTextField(
                value = uiState.weightG,
                onValueChange = onWeightChanged,
                label = { Text("체중 (kg)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 체고
            OutlinedTextField(
                value = uiState.heightCm,
                onValueChange = onHeightChanged,
                label = { Text("체고 (cm)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 중성화 여부
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "중성화", fontSize = 16.sp)
                Switch(
                    checked = uiState.isNeutered,
                    onCheckedChange = onNeuteredChanged
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 메모
            OutlinedTextField(
                value = uiState.memo,
                onValueChange = onMemoChanged,
                label = { Text("메모") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 저장 버튼
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrownPrimary),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "등록하기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileRegisterContentPreview() {
    ProfileRegisterContent(
        uiState = ProfileRegisterUiState(),
        onSaved = {},
        onPhotoSelected = {},
        onRecognizeBreed = {},
        onNameChanged = {},
        onBirthDateChanged = {},
        onGenderSelected = {},
        onBreedNameCustomChanged = {},
        onWeightChanged = {},
        onHeightChanged = {},
        onNeuteredChanged = {},
        onMemoChanged = {},
        onBreedSearchQueryChanged = {},  // 추가
        onBreedSelected = {},
        onSave = {},

    )
}
