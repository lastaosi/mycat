package com.lastaosi.mycat.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.lastaosi.mycat.domain.model.Cat
import com.lastaosi.mycat.domain.model.Gender
import com.lastaosi.mycat.presentation.navigation.NavRoutes
import com.lastaosi.mycat.ui.theme.MyCatColors
import com.lastaosi.mycat.ui.theme.MyCatTheme

// ─── DrawerItem 메타데이터 ────────────────────────────────────────────
private data class DrawerMenuMeta(
    val item: DrawerItem,
    val label: String,
    val emoji: String
)

private val drawerMenuItems = listOf(
    DrawerMenuMeta(DrawerItem.HOME,         "홈",          "🏠"),
    DrawerMenuMeta(DrawerItem.CARE_GUIDE,   "케어 가이드",  "📋"),
    DrawerMenuMeta(DrawerItem.WEIGHT,       "체중 기록",    "⚖️"),
    DrawerMenuMeta(DrawerItem.HEALTH_CHECK, "건강 체크",    "❤️"),
    DrawerMenuMeta(DrawerItem.VACCINATION,  "예방접종",     "💉"),
    DrawerMenuMeta(DrawerItem.MEDICATION,   "약 복용 관리", "💊"),  // 추가
    DrawerMenuMeta(DrawerItem.DIARY,        "다이어리",     "📝"),
    DrawerMenuMeta(DrawerItem.VET_MAP,      "근처 동물병원", "🏥"),
    DrawerMenuMeta(DrawerItem.SETTINGS,     "설정",         "⚙️"),
)

// ─── 1. DrawerContent 컴포넌트 ──────────────────────────────────────
@Composable
fun MainDrawerContent(
    cat: Cat?,
    allCats: List<Cat>,
    selectedItem: DrawerItem,
    onItemClick: (DrawerItem) -> Unit,
    onCatSelected: (Long) -> Unit,      // 추가
    onAddCatClick: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MyCatColors.Background,
        drawerContentColor = MyCatColors.OnBackground
    ) {
        // 헤더 — 고양이 프로필
        DrawerHeader(cat = cat,allCats= allCats,onCatSelected=onCatSelected,onAddCatClick=onAddCatClick,onItemClick)

        HorizontalDivider(
            color = MyCatColors.Border,
            thickness = 0.5.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 메뉴 아이템
        drawerMenuItems.forEach { meta ->
            DrawerMenuItem(
                meta = meta,
                isSelected = selectedItem == meta.item,
                onClick = { onItemClick(meta.item) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 하단 버전 정보
        Text(
            text = "MY Cat v1.0.0",
            fontSize = 11.sp,
            color = MyCatColors.TextMuted,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

// ─── 2. DrawerHeader ────────────────────────────────────────────────
@Composable
private fun DrawerHeader(
    cat: Cat?,
    allCats: List<Cat>,
    onCatSelected: (Long) -> Unit,
    onAddCatClick: () -> Unit,
    onItemClick: (DrawerItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MyCatColors.Surface)
            .padding(20.dp)
    ) {
        // 현재 대표 고양이
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MyCatColors.Border),
                contentAlignment = Alignment.Center
            ) {
                if (cat?.photoPath != null) {
                    AsyncImage(
                        model = cat.photoPath,
                        contentDescription = cat.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(text = "🐱", fontSize = 28.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (cat != null) {
                    Text(
                        text = cat.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MyCatColors.OnBackground
                    )
                    Text(
                        text = cat.breedNameCustom ?: "품종 미등록",
                        fontSize = 12.sp,
                        color = MyCatColors.TextMuted
                    )
                } else {
                    Text(
                        text = "고양이를 등록해주세요",
                        fontSize = 14.sp,
                        color = MyCatColors.TextMuted
                    )
                }
            }

            // 대표 고양이 이름 옆에 수정 아이콘
            IconButton(
                onClick = {
                    onItemClick(DrawerItem.PROFILE_EDIT)
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "프로필 수정",
                    tint = MyCatColors.TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }


        // 고양이 여러 마리일 때 목록 표시
        if (allCats.size > 1) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MyCatColors.Border, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "고양이 전환",
                fontSize = 11.sp,
                color = MyCatColors.TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                allCats.forEach { c ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onCatSelected(c.id) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (c.isRepresentative) MyCatColors.Primary
                                    else MyCatColors.Border
                                )
                                .border(
                                    width = if (c.isRepresentative) 2.dp else 0.dp,
                                    color = MyCatColors.Primary,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (c.photoPath != null) {
                                AsyncImage(
                                    model = c.photoPath,
                                    contentDescription = c.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(text = "🐱", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = c.name,
                            fontSize = 10.sp,
                            color = if (c.isRepresentative) MyCatColors.Primary
                            else MyCatColors.TextMuted,
                            fontWeight = if (c.isRepresentative) FontWeight.Bold
                            else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MyCatColors.Border, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // 고양이 추가 버튼
        TextButton(
            onClick = onAddCatClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "+ 고양이 추가",
                fontSize = 13.sp,
                color = MyCatColors.Primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─── 3. DrawerMenuItem ──────────────────────────────────────────────
@Composable
private fun DrawerMenuItem(
    meta: DrawerMenuMeta,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MyCatColors.Surface else MyCatColors.Background
    val textColor = if (isSelected) MyCatColors.Primary else MyCatColors.OnBackground
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 선택 인디케이터
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (isSelected) MyCatColors.Primary
                    else MyCatColors.Background
                )
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = meta.emoji,
            fontSize = 16.sp,
            modifier = Modifier.size(22.dp),
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = meta.label,
            fontSize = 14.sp,
            fontWeight = fontWeight,
            color = textColor
        )
    }
}

// ─── 4. Preview ─────────────────────────────────────────────────────
@Preview(showBackground = true, widthDp = 280)
@Composable
private fun MainDrawerContentPreview() {
    MyCatTheme {
        MainDrawerContent(
            cat = Cat(
                id = 1L,
                name = "별이",
                birthDate = "2024-09",
                gender = Gender.FEMALE,
                breedNameCustom = "코리안 숏헤어",
                isNeutered = true,
                isRepresentative = true,
                createdAt = 0L
            ),
            selectedItem = DrawerItem.HOME,
            onItemClick = {},
            onCatSelected = {},
            onAddCatClick = {},
            allCats = emptyList()
        )
    }
}

@Preview(showBackground = true, widthDp = 280)
@Composable
private fun MainDrawerContentNoCatPreview() {
    MyCatTheme {
        // 고양이 미등록 상태
        MainDrawerContent(
            cat = null,
            allCats = emptyList(),
            selectedItem = DrawerItem.HOME,
            onItemClick = {},
            onCatSelected = {},
            onAddCatClick = {},
        )
    }
}