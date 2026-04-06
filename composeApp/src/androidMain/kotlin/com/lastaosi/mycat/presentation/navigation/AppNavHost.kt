package com.lastaosi.mycat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lastaosi.mycat.presentation.careguide.CareGuideScreen
import com.lastaosi.mycat.presentation.diary.DiaryScreen
import com.lastaosi.mycat.presentation.healthcheck.HealthCheckScreen
import com.lastaosi.mycat.presentation.main.MainScreen
import com.lastaosi.mycat.presentation.medication.MedicationScreen
import com.lastaosi.mycat.presentation.nearbyvet.NearbyVetScreen
import com.lastaosi.mycat.presentation.profile.ProfileRegisterScreen
import com.lastaosi.mycat.presentation.splash.SplashScreen
import com.lastaosi.mycat.presentation.vaccination.VaccinationScreen
import com.lastaosi.mycat.presentation.weight.WeightScreen

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Splash.route
    ) {
        composable(NavRoutes.Splash.route) {
            SplashScreen(
                onNavigateToMain = {
                    navController.navigate(NavRoutes.Main.route) {
                        popUpTo(NavRoutes.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToProfileRegister = {
                    navController.navigate(NavRoutes.ProfileRegister.route) {
                        popUpTo(NavRoutes.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.ProfileRegister.route) {
            ProfileRegisterScreen(
                onSaved = {
                    navController.navigate(NavRoutes.Main.route) {
                        popUpTo(NavRoutes.ProfileRegister.route) { inclusive = true }
                    }
                }
            )
        }
        // 프로필 수정
        composable(
            route = NavRoutes.ProfileEdit.route,
            arguments = listOf(navArgument("catId") { type = NavType.LongType })
        ) { backStackEntry ->
            val catId = backStackEntry.arguments?.getLong("catId") ?: return@composable
            ProfileRegisterScreen(
                catId = catId,
                onSaved = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.Main.route) {
            MainScreen(navController = navController)
        }

        composable(
            route = NavRoutes.WeightGraph.route,
            arguments = listOf(navArgument("catId") { type = NavType.LongType })
        ) { backStackEntry ->
            val catId = backStackEntry.arguments?.getLong("catId") ?: return@composable
            WeightScreen(
                catId = catId,
                onBack = { navController.popBackStack() }
            )
        }

        // Vaccination 추가
        composable(
            route = NavRoutes.Vaccination.route,
            arguments = listOf(navArgument("catId") { type = NavType.LongType })
        ) { backStackEntry ->
            val catId = backStackEntry.arguments?.getLong("catId") ?: return@composable
            VaccinationScreen(
                catId = catId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.Medication.route,
            arguments = listOf(navArgument("catId") { type = NavType.LongType })
        ) { backStackEntry ->
            val catId = backStackEntry.arguments?.getLong("catId") ?: return@composable
            MedicationScreen(
                catId = catId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.Diary.route,
            arguments = listOf(navArgument("catId") { type = NavType.LongType })
        ) { backStackEntry ->
            val catId = backStackEntry.arguments?.getLong("catId") ?: return@composable
            DiaryScreen(
                catId = catId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.CatHealth.route  // catId 없이 고양이는 ViewModel에서 대표 고양이로
        ) {
            // CatHealth는 catId 파라미터 없이 대표 고양이 기준
            HealthCheckScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.NearbyVet.route) {
            NearbyVetScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.CareGuide.route){
            CareGuideScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}