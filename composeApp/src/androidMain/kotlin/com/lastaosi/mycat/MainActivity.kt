package com.lastaosi.mycat

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.lastaosi.mycat.util.NotificationHelper

class MainActivity : ComponentActivity() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 키패드가 올라올 때 컨텐츠가 밀리도록 설정
        WindowCompat.setDecorFitsSystemWindows(window, false)
// Android 13+ 알림 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        setContent {
            App()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 결과 처리 */ }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}