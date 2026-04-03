package com.lastaosi.mycat.presentation.nearbyvet

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.lastaosi.mycat.ui.theme.MyCatColors
import com.lastaosi.mycat.ui.theme.MyCatTheme
import org.koin.compose.viewmodel.koinViewModel
import android.content.Intent
import android.net.Uri

// ─── 1. Screen ───────────────────────────────────────────────────────
@Composable
fun NearbyVetScreen(
    onBack: () -> Unit,
    viewModel: NearbyVetViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // 위치 권한 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) viewModel.loadNearbyVets()
    }

    // 권한 확인 후 자동 검색
    LaunchedEffect(Unit) {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (fineLocation == PermissionChecker.PERMISSION_GRANTED) {
            viewModel.loadNearbyVets()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    NearbyVetContent(
        uiState = uiState,
        onBack = onBack,
        onVetSelected = viewModel::onVetSelected,
        onRetry = viewModel::onRetry,
        onCallClick = { phone ->
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            context.startActivity(intent)
        },
        onNavigateClick = { latLng ->
            val uri = Uri.parse("google.navigation:q=${latLng.latitude},${latLng.longitude}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
            }
            context.startActivity(intent)
        }
    )
}

// ─── 2. Content ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyVetContent(
    uiState: NearbyVetUiState,
    onBack: () -> Unit,
    onVetSelected: (VetPlace?) -> Unit,
    onRetry: () -> Unit,
    onCallClick: (String) -> Unit,
    onNavigateClick: (LatLng) -> Unit
) {
    val defaultLocation = LatLng(37.5665, 126.9780)  // 서울 기본값
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            uiState.currentLocation ?: defaultLocation, 14f
        )
    }

    // 현재 위치 로드되면 카메라 이동
    LaunchedEffect(uiState.currentLocation) {
        uiState.currentLocation?.let { location ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(location, 14f)
            )
        }
    }

    // 선택된 병원으로 카메라 이동
    LaunchedEffect(uiState.selectedVet) {
        uiState.selectedVet?.let { vet ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(vet.latLng, 16f)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "근처 동물병원",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MyCatColors.OnPrimary
                    )
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
            // 상단 지도 (40%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = uiState.currentLocation != null),
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = true,
                        zoomControlsEnabled = false
                    )
                ) {
                    // 병원 마커
                    uiState.vets.forEach { vet ->
                        Marker(
                            state = MarkerState(position = vet.latLng),
                            title = vet.name,
                            snippet = vet.address,
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (uiState.selectedVet?.placeId == vet.placeId)
                                    BitmapDescriptorFactory.HUE_ORANGE
                                else BitmapDescriptorFactory.HUE_RED
                            ),
                            onClick = {
                                onVetSelected(vet)
                                false
                            }
                        )
                    }
                }

                // 로딩
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MyCatColors.Primary)
                    }
                }
            }

            // 하단 목록 (60%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
            ) {
                when {
                    uiState.errorMessage != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = uiState.errorMessage,
                                color = MyCatColors.TextMuted,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onRetry,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MyCatColors.Primary
                                )
                            ) {
                                Text("다시 시도", color = MyCatColors.OnPrimary)
                            }
                        }
                    }
                    uiState.vets.isEmpty() && !uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "근처 동물병원이 없어요",
                                color = MyCatColors.TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 검색 결과 수
                            item {
                                Text(
                                    text = "근처 동물병원 ${uiState.vets.size}곳",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MyCatColors.OnBackground
                                )
                            }
                            items(
                                items = uiState.vets,
                                key = { it.placeId }
                            ) { vet ->
                                VetItem(
                                    vet = vet,
                                    isSelected = uiState.selectedVet?.placeId == vet.placeId,
                                    onClick = { onVetSelected(vet) },
                                    onCallClick = { vet.phoneNumber?.let { onCallClick(it) } },
                                    onNavigateClick = { onNavigateClick(vet.latLng) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── 3. VetItem ──────────────────────────────────────────────────────
@Composable
private fun VetItem(
    vet: VetPlace,
    isSelected: Boolean,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    onNavigateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MyCatColors.Surface else MyCatColors.OnPrimary
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 3.dp else 1.dp),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // 병원명
                    Text(
                        text = vet.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MyCatColors.OnBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // 주소
                    Text(
                        text = vet.address,
                        fontSize = 12.sp,
                        color = MyCatColors.TextMuted
                    )
                }

                // 거리
                vet.distance?.let { distance ->
                    Text(
                        text = if (distance >= 1000f)
                            "${"%.1f".format(distance / 1000f)}km"
                        else
                            "${distance.toInt()}m",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MyCatColors.Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 별점
                vet.rating?.let { rating ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⭐", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "%.1f".format(rating),
                            fontSize = 12.sp,
                            color = MyCatColors.OnBackground
                        )
                    }
                }

                // 영업 상태
                vet.isOpen?.let { isOpen ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isOpen) MyCatColors.Success.copy(alpha = 0.15f)
                                else MyCatColors.TextMuted.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isOpen) "영업중" else "영업종료",
                            fontSize = 11.sp,
                            color = if (isOpen) MyCatColors.Success else MyCatColors.TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 전화 버튼
                if (vet.phoneNumber != null) {
                    IconButton(
                        onClick = onCallClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "전화",
                            tint = MyCatColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 길찾기 버튼
                IconButton(
                    onClick = onNavigateClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = "길찾기",
                        tint = MyCatColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}