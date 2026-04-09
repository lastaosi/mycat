package com.lastaosi.mycat.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lastaosi.mycat.R
import com.lastaosi.mycat.presentation.profile.ProfileRegisterContent
import com.lastaosi.mycat.presentation.profile.ProfileRegisterUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.text.toInt

private val SplashBackground = Color(0xFFFFF8F0)

@Composable
fun SplashScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToProfileRegister: () -> Unit,
    viewModel: SplashViewModel = koinViewModel()
) {
    val destination by viewModel.destination.collectAsState()



    // ViewModel 상태에 따라 화면 전환
    LaunchedEffect(destination) {
        when (destination) {
            is SplashDestination.Main -> onNavigateToMain()
            is SplashDestination.ProfileRegister -> onNavigateToProfileRegister()
            SplashDestination.Loading -> Unit
        }
    }

    SplashContent()

}

/**
 * 스플래시 애니메이션 화면.
 *
 * ## 고양이 4마리 배치 및 애니메이션
 *
 * | 이미지 | 위치 기준점 | 애니메이션 |
 * |-------|------------|-----------|
 * | sit   | BottomCenter | 고정 (정적) |
 * | roll  | Center + y(-120dp) | 고정 (정적) |
 * | walk  | Center + y(+180px) | 좌우 왕복 |
 * | run   | Center + (runOffsetX, runOffsetY) | 사각형 영역 내 랜덤 튕기기 |
 *
 * ## walk 애니메이션 (좌우 왕복)
 * - walkOffsetX 는 -400f ~ 400f 범위를 2초 선형 이동
 * - 목적지 도달 시 방향 전환(walkGoingRight 토글) + 80ms 대기 (끝에서 살짝 멈춤 효과)
 * - scaleX: 오른쪽 이동 시 1f (정방향), 왼쪽 이동 시 -1f (좌우 반전)
 *
 * ## run 애니메이션 (랜덤 튕기기)
 * - 매 프레임(16ms = ~60fps) 마다 (runDirX * 8f, runDirY * 8f) 씩 이동
 * - |nextX| >= 380f 이면 x 방향 반전, |nextY| >= 700f 이면 y 방향 반전
 * - x, y 를 동시에 이동시키되 x 는 별도 launch 로 병렬 실행해 프레임 지연 방지
 * - scaleX 로 이동 방향에 따라 이미지 좌우 반전
 */
@Composable
fun SplashContent() {
    // walk: 좌우 왕복 (초기 위치: 화면 왼쪽 바깥 -400f)
    val walkOffsetX = remember { Animatable(-400f) }
    var walkGoingRight by remember { mutableStateOf(true) }

    // run: 2D 랜덤 튕기기 (초기 위치: 중앙 0,0)
    val runOffsetX = remember { Animatable(0f) }
    val runOffsetY = remember { Animatable(0f) }
    var runDirX by remember { mutableFloatStateOf(1f) }   // +1 = 오른쪽, -1 = 왼쪽
    var runDirY by remember { mutableFloatStateOf(1f) }   // +1 = 아래쪽, -1 = 위쪽
    var runFacingRight by remember { mutableStateOf(true) }

    // walk 좌우 왕복 루프
    LaunchedEffect(Unit) {
        while (true) {
            val target = if (walkGoingRight) 400f else -400f
            walkOffsetX.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
            )
            walkGoingRight = !walkGoingRight
            delay(80)  // 방향 전환 전 살짝 멈춤 효과
        }
    }

    // run 랜덤 튕기기 루프 (~60fps, 16ms 간격)
    LaunchedEffect(Unit) {
        while (true) {
            val nextX = runOffsetX.value + runDirX * 8f
            val nextY = runOffsetY.value + runDirY * 8f

            // 수평 경계(±380f) 도달 시 x 방향 반전 + 이미지 좌우 반전
            if (kotlin.math.abs(nextX) >= 380f) {
                runDirX = -runDirX
                runFacingRight = runDirX > 0
            }
            // 수직 경계(±700f) 도달 시 y 방향 반전
            if (kotlin.math.abs(nextY) >= 700f) {
                runDirY = -runDirY
            }

            // x, y 를 병렬로 이동시켜 프레임 지연 없이 대각선 이동
            launch {
                runOffsetX.animateTo(
                    targetValue = runOffsetX.value + runDirX * 8f,
                    animationSpec = tween(durationMillis = 16, easing = LinearEasing)
                )
            }
            runOffsetY.animateTo(
                targetValue = runOffsetY.value + runDirY * 8f,
                animationSpec = tween(durationMillis = 16, easing = LinearEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBackground)
    ) {
        // 1. sit — 하단 중앙 고정
        Image(
            painter = painterResource(id = R.drawable.byeori_sit),
            contentDescription = "sit",
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
        )

        // 2. roll — 화면 중앙에서 120dp 위에 고정
        Image(
            painter = painterResource(id = R.drawable.byeori_roll),
            contentDescription = "roll",
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.Center)
                .offset(y = (-120).dp)
        )

        // 3. walk — 중앙 기준 y+180px 고정 높이에서 좌우 왕복
        Image(
            painter = painterResource(id = R.drawable.byeori_walk),
            contentDescription = "walk",
            modifier = Modifier
                .size(130.dp)
                .align(Alignment.Center)
                .offset { IntOffset(x = walkOffsetX.value.toInt(), y = 180) }
                .scale(scaleX = if (walkGoingRight) 1f else -1f, scaleY = 1f)  // 방향에 따라 좌우 반전
        )

        // 4. run — 중앙 기준으로 2D 자유 이동 (사각형 경계 내 튕기기)
        Image(
            painter = painterResource(id = R.drawable.byeori_run),
            contentDescription = "run",
            modifier = Modifier
                .size(130.dp)
                .align(Alignment.Center)
                .offset {
                    IntOffset(
                        x = runOffsetX.value.toInt(),
                        y = runOffsetY.value.toInt()
                    )
                }
                .scale(scaleX = if (runFacingRight) 1f else -1f, scaleY = 1f)  // 이동 방향에 따라 좌우 반전
        )

        // 5. 로고 텍스트
        Text(
            text = "My Cat",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B5E3C),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 140.dp)
        )

        // 6. 로딩 안내 텍스트
        Text(
            text = "초기 데이터를 생성중입니다..",
            fontSize = 13.sp,
            color = Color(0xFFAA8866),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F0)
@Composable
fun SplashContentPreview() {
    SplashContent()
}