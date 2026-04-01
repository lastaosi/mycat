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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lastaosi.mycat.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.random.Random

private val SplashBackground = Color(0xFFFFF8F0)

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {

    // --- walk: 좌우 왕복 ---
    val walkOffsetX = remember { Animatable(-400f) }
    var walkGoingRight by remember { mutableStateOf(true) }

    // --- run: 랜덤 방향으로 화면 튕기기 ---
    val runOffsetX = remember { Animatable(0f) }
    val runOffsetY = remember { Animatable(0f) }
    var runDirX by remember { mutableFloatStateOf(1f) }
    var runDirY by remember { mutableFloatStateOf(1f) }
    var runFacingRight by remember { mutableStateOf(true) }

    // walk 좌우 왕복
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
            val stepX = runDirX * 8f
            val stepY = runDirY * 8f
            val nextX = runOffsetX.value + stepX
            val nextY = runOffsetY.value + stepY

            // X 경계 튕기기
            if (nextX.absoluteValue >= 380f) {
                runDirX = -runDirX
                runFacingRight = runDirX > 0
            }
            // Y 경계 튕기기
            if (nextY.absoluteValue >= 700f) {
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

    // 시드 완료 후 화면 전환 (나중에 ViewModel로 교체)
    LaunchedEffect(Unit) {
        delay(3000)
        onSplashFinished()
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
                .size(200.dp)          // 120 → 200
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
        )

        // 2. roll — 중앙 상단 고정
        Image(
            painter = painterResource(id = R.drawable.byeori_roll),
            contentDescription = "roll",
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.Center)  // TopCenter → Center
                .offset(y = (-120).dp)    // 중앙에서 위로 올리기
        )

        // 3. walk — 좌우 왕복 (화면 중하단)
        Image(
            painter = painterResource(id = R.drawable.byeori_walk),
            contentDescription = "walk",
            modifier = Modifier
                .size(130.dp)          // 100 → 130
                .align(Alignment.Center)
                .offset { IntOffset(x = walkOffsetX.value.toInt(), y = 180) }
                .scale(scaleX = if (walkGoingRight) 1f else -1f, scaleY = 1f)
        )

        // 4. run — 랜덤 방향 튕기기 (화면 전체)
        Image(
            painter = painterResource(id = R.drawable.byeori_run),
            contentDescription = "run",
            modifier = Modifier
                .size(130.dp)          // 100 → 130
                .align(Alignment.Center)
                .offset {
                    IntOffset(
                        x = runOffsetX.value.toInt(),
                        y = runOffsetY.value.toInt()
                    )
                }
                .scale(scaleX = if (runFacingRight) 1f else -1f, scaleY = 1f)
        )

        // 5. 로고 텍스트 — 중앙
        Text(
            text = "My Cat",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B5E3C),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 140.dp)
        )

        // 6. 초기 데이터 생성 텍스트 — 최하단
        Text(
            text = "초기 데이터를 생성중입니다..",
            fontSize = 13.sp,
            color = Color(0xFFAA8866),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}