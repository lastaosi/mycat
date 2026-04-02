package com.lastaosi.mycat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lastaosi.mycat.presentation.profile.ProfileRegisterScreen
import com.lastaosi.mycat.presentation.splash.SplashScreen

/**
 * 앱 전체 Navigation 그래프.
 * Splash → ProfileRegister (첫 실행) 또는 Main (기존 사용자)
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = NavRoutes.Splash.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
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
            // 추후 구현
        }
    }
}