package com.lastaosi.mycat.presentation.weight

sealed class WeightAction {
    data class TabSelected(val tab: WeightTab) : WeightAction()
    data object FabClick : WeightAction()
    data object DialogDismiss : WeightAction()
    data class WeightSave(val weightKg: String, val memo: String) : WeightAction()
}