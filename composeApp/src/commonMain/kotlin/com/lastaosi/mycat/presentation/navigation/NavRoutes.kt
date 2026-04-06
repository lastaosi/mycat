package com.lastaosi.mycat.presentation.navigation

sealed class NavRoutes(val route: String) {
    data object Splash : NavRoutes("splash")
    data object ProfileRegister : NavRoutes("profile_register")
    data object Main : NavRoutes("main")
    data object ProfileEdit : NavRoutes("profile_edit/{catId}") {
        fun createRoute(catId: Long) = "profile_edit/$catId"
    }
    data object WeightGraph : NavRoutes("weight_graph/{catId}") {
        fun createRoute(catId: Long) = "weight_graph/$catId"
    }
    data object Vaccination : NavRoutes("vaccination/{catId}") {
        fun createRoute(catId: Long) = "vaccination/$catId"
    }
    data object Medication : NavRoutes("medication/{catId}") {
        fun createRoute(catId: Long) = "medication/$catId"
    }
    data object Diary : NavRoutes("diary/{catId}") {
        fun createRoute(catId: Long) = "diary/$catId"
    }
    data object BreedGuide : NavRoutes("breed_guide")
    data object CatHealth : NavRoutes("cat_health")
    data object NearbyVet : NavRoutes("nearby_vet")
    data object CatInfo : NavRoutes("cat_info")
    data object DonationCoffee : NavRoutes("donation_coffee")
    data object CareGuide : NavRoutes("care_guide")

}