package com.eggetteluo.folder.ui.navigation

import android.os.Environment
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.eggetteluo.folder.ui.features.explorer.ExplorerScreen
import com.eggetteluo.folder.ui.features.login.LoginScreen
import com.eggetteluo.folder.ui.features.permission.PermissionScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    contentPadding: PaddingValues
) {
    val startDest = if (Environment.isExternalStorageManager()) {
        Screen.Login.route
    } else {
        Screen.Permission.route
    }

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        // 权限页
        composable(Screen.Permission.route) {
            PermissionScreen(onPermissionGranted = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Permission.route) { inclusive = true }
                }
            })
        }

        // 登录页面路由
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { userId ->
                    navController.navigate(Screen.Explorer.createRoute(userId)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // 文件管理页面路由
        composable(Screen.Explorer.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ExplorerScreen(
                userId = userId,
                contentPadding = contentPadding
            )
        }
    }
}