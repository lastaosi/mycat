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

@Composable
fun SplashContent() {
    // 기존 SplashScreen UI 코드 전체 여기로 이동
    // walkOffsetX, runOffsetX 등 애니메이션 코드 포함
    // walk 좌우 왕복
    val walkOffsetX = remember { Animatable(-400f) }
    var walkGoingRight by remember { mutableStateOf(true) }

    // run 랜덤 튕기기
    val runOffsetX = remember { Animatable(0f) }
    val runOffsetY = remember { Animatable(0f) }
    var runDirX by remember { mutableFloatStateOf(1f) }
    var runDirY by remember { mutableFloatStateOf(1f) }
    var runFacingRight by remember { mutableStateOf(true) }

    // walk 애니메이션
    LaunchedEffect(Unit) {
        while (true) {
            val target = if (walkGoingRight) 400f else -400f
            walkOffsetX.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
            )
            walkGoingRight = !walkGoingRight
            delay(80)
        }
    }

    // run 랜덤 튕기기
    LaunchedEffect(Unit) {
        while (true) {
            val nextX = runOffsetX.value + runDirX * 8f
            val nextY = runOffsetY.value + runDirY * 8f
            if (kotlin.math.abs(nextX) >= 380f) {
                runDirX = -runDirX
                runFacingRight = runDirX > 0
            }
            if (kotlin.math.abs(nextY) >= 700f) {
                runDirY = -runDirY
            }
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
        // 1. sit — 하단 고정
        Image(
            painter = painterResource(id = R.drawable.byeori_sit),
            contentDescription = "sit",
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
        )

        // 2. roll — 중앙에서 위로
        Image(
            painter = painterResource(id = R.drawable.byeori_roll),
            contentDescription = "roll",
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.Center)
                .offset(y = (-120).dp)
        )

        // 3. walk — 좌우 왕복
        Image(
            painter = painterResource(id = R.drawable.byeori_walk),
            contentDescription = "walk",
            modifier = Modifier
                .size(130.dp)
                .align(Alignment.Center)
                .offset { IntOffset(x = walkOffsetX.value.toInt(), y = 180) }
                .scale(scaleX = if (walkGoingRight) 1f else -1f, scaleY = 1f)
        )

        // 4. run — 랜덤 튕기기
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
                .scale(scaleX = if (runFacingRight) 1f else -1f, scaleY = 1f)
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

        // 6. 로딩 텍스트
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