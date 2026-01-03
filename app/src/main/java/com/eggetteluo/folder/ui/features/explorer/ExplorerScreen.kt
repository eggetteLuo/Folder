package com.eggetteluo.folder.ui.features.explorer

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eggetteluo.folder.model.FileItem
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(userId: String) {
    val viewModel: ExplorerViewModel = viewModel()
    val fileList by viewModel.fileList.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val isRoot by remember(currentPath) {
        derivedStateOf { viewModel.isRoot() }
    }
    val isClipboardEmpty by viewModel.clipboardFile.collectAsState()

    val showDialog = remember { mutableStateOf(false) }
    val newFolderName = remember { mutableStateOf("") }

    val showMenu = remember { mutableStateOf(false) }
    val selectedFile = remember { mutableStateOf<FileItem?>(null) }

    val showRenameDialog = remember { mutableStateOf(false) }
    val renameTextFieldValue = remember { mutableStateOf("") }

    val showDetailsSheet = remember { mutableStateOf(false) }

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
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isRoot) "我的空间 ($userId)" else currentPath?.name ?: "",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                windowInsets = TopAppBarDefaults.windowInsets,
                navigationIcon = {
                    if (!isRoot) {
                        IconButton(onClick = { viewModel.navigateBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                        IconButton(onClick = { showMenu.value = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                        }

                        // 下拉菜单组件
                        DropdownMenu(
                            expanded = showMenu.value,
                            onDismissRequest = { showMenu.value = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("刷新列表") },
                                onClick = {
                                    showMenu.value = false
                                    viewModel.currentPath.value?.let { viewModel.loadFiles(it) }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("按名称排序") },
                                onClick = {
                                    showMenu.value = false
                                    viewModel.updateSortOrder(ExplorerViewModel.SortOrder.NAME)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("按时间排序 (最新优先)") },
                                onClick = {
                                    showMenu.value = false
                                    viewModel.updateSortOrder(ExplorerViewModel.SortOrder.TIME_DESC)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("按时间排序 (最旧优先)") },
                                onClick = {
                                    showMenu.value = false
                                    viewModel.updateSortOrder(ExplorerViewModel.SortOrder.TIME_ASC)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("粘贴") },
                                enabled = isClipboardEmpty != null,
                                onClick = {
                                    // 粘贴事件
                                    showMenu.value = false
                                    currentPath?.let {
                                        viewModel.paste(it)
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (selectedFile.value == null) {
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
        },
        bottomBar = {
            if (selectedFile.value != null) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary,
                    actions = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ActionIconButton(Icons.Default.Delete, "删除") {
                                selectedFile.value?.let {
                                    viewModel.deleteItem(it)
                                }
                                selectedFile.value = null
                            }
                            ActionIconButton(Icons.Default.Info, "详情") {
                                showDetailsSheet.value = true
                            }
                            ActionIconButton(Icons.Default.ContentCut, "剪切") {
                                // 剪切逻辑
                                selectedFile.value?.let {
                                    viewModel.setClipboard(it, ExplorerViewModel.TransferMode.CUT)
                                }
                                selectedFile.value = null
                            }
                            ActionIconButton(Icons.Default.ContentCopy, "复制") {
                                // 复制逻辑
                                selectedFile.value?.let {
                                    viewModel.setClipboard(it, ExplorerViewModel.TransferMode.COPY)
                                }
                                selectedFile.value = null
                            }
                            ActionIconButton(Icons.Default.Edit, "重命名") {
                                selectedFile.value?.let {
                                    renameTextFieldValue.value = it.name // 默认填充当前文件名
                                    showRenameDialog.value = true
                                }
                            }
                            ActionIconButton(Icons.Default.Cancel, "取消") {
                                selectedFile.value = null
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            if (fileList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("文件夹为空", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(fileList) { file ->
                        FileRow(
                            file = file,
                            onClick = {
                                if (selectedFile.value != null) {
                                    // 如果当前处于选中模式，点击任何项可能意味着取消选中
                                    selectedFile.value = null
                                } else {
                                    // 正常的目录跳转或打开文件逻辑
                                    if (file.isDirectory) viewModel.loadFiles(File(file.path))
                                    else viewModel.openFile(context, file)
                                }
                            },
                            onLongClick = {
                                // 长按时，记录选中的文件，这会触发 Scaffold 底部工具栏的弹出
                                selectedFile.value = file
                            }
                        )
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

        if (showRenameDialog.value && selectedFile.value != null) {
            AlertDialog(
                onDismissRequest = { showRenameDialog.value = false },
                title = { Text("重命名") },
                text = {
                    OutlinedTextField(
                        value = renameTextFieldValue.value,
                        onValueChange = { renameTextFieldValue.value = it },
                        label = { Text("新名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (renameTextFieldValue.value.isNotBlank() &&
                                renameTextFieldValue.value != selectedFile.value?.name) {
                                viewModel.renameItem(selectedFile.value!!, renameTextFieldValue.value)
                                selectedFile.value = null // 操作完取消选中状态
                                showRenameDialog.value = false
                            }
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog.value = false }) {
                        Text("取消")
                    }
                }
            )
        }

        if (showDetailsSheet.value && selectedFile.value != null) {
            FileDetailsSheet(
                file = selectedFile.value!!,
                onDismiss = { showDetailsSheet.value = false }
            )
        }
    }
}