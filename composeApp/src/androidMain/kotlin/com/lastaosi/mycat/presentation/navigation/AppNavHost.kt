package com.lastaosi.mycat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lastaosi.mycat.presentation.main.MainScreen
import com.lastaosi.mycat.presentation.profile.ProfileRegisterScreen
import com.lastaosi.mycat.presentation.splash.SplashScreen
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

        composable(NavRoutes.Main.route) {
            MainScreen(navController = navController)
        }

        // WeightGraph 추가
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
    }
}