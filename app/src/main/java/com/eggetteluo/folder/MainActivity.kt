package com.eggetteluo.folder

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.eggetteluo.folder.ui.navigation.AppNavGraph
import com.eggetteluo.folder.ui.theme.FolderTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FolderTheme {
                FolderTheme {
                    val navController = rememberNavController()
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AppNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}