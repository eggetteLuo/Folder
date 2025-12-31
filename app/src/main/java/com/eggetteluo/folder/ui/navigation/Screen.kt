package com.eggetteluo.folder.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Explorer : Screen("explorer/{userId}") {
        fun createRoute(userId: String) = "explorer/$userId"
    }
}