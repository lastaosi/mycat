package com.lastaosi.mycat.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.lastaosi.mycat.domain.model.Cat
import com.lastaosi.mycat.domain.model.Gender
import com.lastaosi.mycat.presentation.navigation.NavRoutes
import com.lastaosi.mycat.ui.theme.MyCatColors
import com.lastaosi.mycat.ui.theme.MyCatTheme
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

// ─── 1. Screen: ViewModel 주입 (Preview 불가) ────────────────────────
// MainScreen
@Composable
fun MainScreen(
    navController: NavHostController,
    viewModel: MainViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    MainContent(
        uiState = uiState,
        onAction = { action ->
            when (action) {
                // Navigation 관련은 Screen에서 처리
                is MainAction.Navigate -> {
                    val catId = uiState.cat?.id ?: return@MainContent
                    when (action.item) {
                        DrawerItem.WEIGHT      -> navController.navigate(NavRoutes.WeightGraph.createRoute(catId))
                        DrawerItem.VACCINATION -> navController.navigate(NavRoutes.Vaccination.createRoute(catId))
                        DrawerItem.DIARY       -> navController.navigate(NavRoutes.Diary.createRoute(catId))
                        DrawerItem.MEDICATION  -> navController.navigate(NavRoutes.Medication.createRoute(catId))
                        DrawerItem.VET_MAP     -> navController.navigate(NavRoutes.NearbyVet.route)
                        DrawerItem.HEALTH_CHECK -> navController.navigate(NavRoutes.CatHealth.route)
                        DrawerItem.PROFILE_EDIT -> {
                            uiState.cat?.id?.let {
                                navController.navigate(NavRoutes.ProfileEdit.createRoute(it))
                            }
                        }
                        DrawerItem.CARE_GUIDE -> navController.navigate(NavRoutes.CareGuide.route)
                        else -> viewModel.onAction(action)
                    }
                }
                is MainAction.AddCatClick -> {
                    navController.navigate(NavRoutes.ProfileRegister.route)
                }
                // 나머지는 ViewModel로 위임
                else -> viewModel.onAction(action)
            }
        }
    )
}

// ─── 2. Content: 순수 UI (Preview 가능) ─────────────────────────────
@Composable
fun MainContent(
    uiState: MainUiState,
    onAction: (MainAction) -> Unit  // 파라미터 1개로 통일
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MainDrawerContent(
                cat = uiState.cat,
                allCats = uiState.allCats,
                selectedItem = uiState.selectedDrawerItem,
                onAction = { action ->
                    scope.launch { drawerState.close() }
                    onAction(action)
                }
            )
        },
        scrimColor = MyCatColors.OnBackground.copy(alpha = 0.3f)
    ) {
        Scaffold(
            topBar = {
                MainTopBar(
                    catName = uiState.cat?.name,
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                        onAction(MainAction.MenuClick)
                    }
                )
            },
            containerColor = MyCatColors.Background
        ) { innerPadding ->
            MainScrollContent(
                uiState = uiState,
                onAction = onAction,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}



// ─── 3. Preview ─────────────────────────────────────────────────────
@Preview(showBackground = true)
@Composable
private fun MainContentPreview() {
    MyCatTheme {
        MainContent(
            uiState = MainUiState(
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
                todayFoodDryG = 45,
                todayFoodWetG = 30,
                todayWaterMl = 210,
                weightMinG = 2100,
                weightMaxG = 2400,
                latestWeightG = 2300,
                randomTip = "매달 구충제로 심장사상충과 기생충 방어하기",
                upcomingVaccinations = listOf(
                    UpcomingAlarm("3차 종합백신", "D-3", isUrgent = true),
                    UpcomingAlarm("광견병 예방접종", "D-14", isUrgent = false)
                ),
                upcomingMedications = listOf(
                    UpcomingAlarm("항생제", "오늘 18:00", isUrgent = true)
                ),
                recentDiaries = listOf(
                    DiaryPreview(1L, "오늘의 별이", "오늘 레오가 새벽에 기세차게 세게 이도린 장난감 표를 갖고 싶어 해서...", "😸", "2026.04.01"),
                    DiaryPreview(2L, "병원 다녀온 날", "정기검진 결과 건강하다고 해서 다행이었다.", "😊", "2026.03.28")
                ),
            ),

            onAction = {}

        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainContentNoCatPreview() {
    MyCatTheme {
        MainContent(
            uiState = MainUiState(),
            onAction = {}
        )
    }
}