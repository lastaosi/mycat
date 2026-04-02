package com.lastaosi.mycat.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lastaosi.mycat.ui.theme.MyCatColors
import com.lastaosi.mycat.ui.theme.MyCatTheme

// ─── 1. TopBar 컴포넌트 ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    catName: String?,
    onMenuClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "MY Cat",
                    fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MyCatColors.OnPrimary
                )
                catName?.let {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = MyCatColors.OnPrimary.copy(alpha = 0.85f)
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "메뉴",
                    tint = MyCatColors.OnPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MyCatColors.Primary
        )
    )
}

// ─── 2. Preview ─────────────────────────────────────────────────────
@Preview(showBackground = true)
@Composable
private fun MainTopBarPreview() {
    MyCatTheme {
        MainTopBar(
            catName = "별이",
            onMenuClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainTopBarNoCatPreview() {
    MyCatTheme {
        // 고양이 미등록 상태
        MainTopBar(
            catName = null,
            onMenuClick = {}
        )
    }
}