package com.eggetteluo.folder.ui.features.explorer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

@Composable
fun ExplorerScreen(userId: String) {
    val viewModel: ExplorerViewModel = viewModel()
    val fileList by viewModel.fileList.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()

    // 关键：当 userId 改变时，初始化空间
    LaunchedEffect(userId) {
        viewModel.initUserSpace(userId)
    }

    Column {
        // 使用来自 components.kt 的组件
        currentPath?.let { PathHeader(it.absolutePath) }

        LazyColumn {
            items(fileList) { file ->
                FileRow(file = file) {
                    if (file.isDirectory) {
                        viewModel.loadFiles(File(file.path))
                    }
                }
            }
        }
    }
}