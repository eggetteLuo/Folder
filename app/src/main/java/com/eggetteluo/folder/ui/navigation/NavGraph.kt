package com.eggetteluo.folder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.eggetteluo.folder.ui.features.explorer.ExplorerScreen
import com.eggetteluo.folder.ui.features.login.LoginScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
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
            // 这里建议在 features/explorer 下创建专门的页面入口
            ExplorerScreen(userId = userId)
        }
    }
}