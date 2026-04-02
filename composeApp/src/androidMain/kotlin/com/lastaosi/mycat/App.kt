package com.lastaosi.mycat

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.lastaosi.mycat.presentation.navigation.AppNavHost

@Composable
fun App() {
    val navController = rememberNavController()
    AppNavHost(navController = navController)
}