package com.eggetteluo.folder.ui.features.explorer

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

@Composable
fun ExplorerScreen(userId: String) {
    val viewModel: ExplorerViewModel = viewModel()
    val fileList by viewModel.fileList.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()

    val showDialog = remember { mutableStateOf(false) }
    val newFolderName = remember { mutableStateOf("") }

    val context = LocalContext.current
    val activity = context as? Activity

    // 关键：当 userId 改变时，初始化空间
    LaunchedEffect(userId) {
        viewModel.initUserSpace(userId)
    }

    BackHandler(enabled = true) {
        // 尝试回退目录
        val handled = viewModel.navigateBack()
        if (!handled) {
            // 如果已经在根目录无法回退了，则退出 Activity (应用)
            activity?.finish()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showDialog.value = true
                    newFolderName.value = ""
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建文件")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            // 使用来自 components.kt 的组件
            currentPath?.let { PathHeader(it.absolutePath) }

            LazyColumn {
                items(fileList) { file ->
                    FileRow(file = file) {
                        if (file.isDirectory) {
                            // 如果是文件夹，加载内容
                            viewModel.loadFiles(File(file.path))
                        } else {
                            // 如果是文件，尝试打开
                            viewModel.openFile(context, file)
                        }
                    }
                }
            }
        }

        if (showDialog.value) {
            AlertDialog(
                onDismissRequest = { showDialog.value = false },
                title = { Text("新建文件") },
                text = {
                    OutlinedTextField(
                        value = newFolderName.value,
                        onValueChange = { newFolderName.value = it },
                        label = { Text("文件名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newFolderName.value.isNotBlank()) {
                                // 调用 ViewModel 执行真正的文件系统操作
                                viewModel.createItem(newFolderName.value)
                                showDialog.value = false
                            }
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}