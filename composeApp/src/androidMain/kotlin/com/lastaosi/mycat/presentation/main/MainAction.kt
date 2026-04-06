package com.lastaosi.mycat.presentation.main

sealed class MainAction {
    // 드로어
    data object MenuClick: MainAction()
    data object AddCatClick : MainAction()
    data class CatSelected(val catId: Long) : MainAction()
    data class DrawerItemClick(val item: DrawerItem) : MainAction()
    // 홈
    data object RefreshTip : MainAction()

    // 네비게이션
    data class Navigate(val item: DrawerItem) : MainAction()
}

