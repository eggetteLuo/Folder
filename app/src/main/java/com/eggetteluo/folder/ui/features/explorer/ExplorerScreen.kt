package com.eggetteluo.folder.ui.features.explorer

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Update
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eggetteluo.folder.model.FileItem
import java.io.File

/**
 * 文件管理器主界面
 * 使用 Material3 规范实现的响应式文件列表页面
 * * @param userId 用户ID，用于区分不同用户的存储空间
 * @param onLogout 退出登录的回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(userId: String, onLogout: () -> Unit) {
    // --- 状态与业务逻辑订阅 ---
    val viewModel: ExplorerViewModel = viewModel()

    // collectAsState 将 Flow 转化为 Compose 可感知的 State，实现数据驱动 UI
    val fileList by viewModel.fileList.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val clipboardFile by viewModel.clipboardFile.collectAsState()

    // 使用 derivedStateOf 优化性能：只有当 currentPath 变化时才重新计算是否为根目录
    val isRoot by remember(currentPath) { derivedStateOf { viewModel.isRoot() } }

    // --- UI 交互状态管理 ---
    val showDialog = remember { mutableStateOf(false) }         // 控制新建文件对话框
    val showMenu = remember { mutableStateOf(false) }           // 控制顶部下拉菜单
    val selectedFile = remember { mutableStateOf<FileItem?>(null) } // 当前被选中的文件（长按触发）
    val showRenameDialog = remember { mutableStateOf(false) }   // 控制重命名对话框
    val showDetailsSheet = remember { mutableStateOf(false) }   // 控制详情底部抽屉
    val snackbarHostState = remember { SnackbarHostState() }    // 管理 Snackbar 的显示队列

    val context = LocalContext.current
    val activity = context as? Activity

    // 滚动行为配置：当列表滚动时，TopAppBar 的颜色和阴影会产生自然变化
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // --- 生命周期与异步事件监听 ---

    // 初始化用户空间，当 userId 变化时重新加载
    LaunchedEffect(userId) {
        viewModel.initUserSpace(userId)
    }

    // 监听 ViewModel 发出的单次错误事件，并以 Snackbar 形式展示
    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
        }
    }

    // 处理系统返回键：如果有选中文件先取消选中；否则尝试回退目录；最后关闭应用
    BackHandler(enabled = true) {
        if (selectedFile.value != null) selectedFile.value = null
        else if (!viewModel.navigateBack()) activity?.finish()
    }

    // 注册系统相册选择器的回调
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { viewModel.saveImageToCurrentDir(context, it) }
    }

    // --- 界面布局主体 ---
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection), // 绑定嵌套滚动
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isRoot) "我的空间" else currentPath?.name ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isRoot) {
                            Text(
                                userId,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    // 非根目录下显示返回箭头
                    if (!isRoot) {
                        IconButton(onClick = { viewModel.navigateBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    // 如果剪贴板有内容，在顶部显示“快捷粘贴”图标
                    if (clipboardFile != null) {
                        IconButton(onClick = { currentPath?.let { viewModel.paste(it) } }) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "粘贴",
                                tint = Color(0xFFFF9800)
                            )
                        }
                    }

                    IconButton(onClick = { showMenu.value = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                    }

                    // 更多选项下拉菜单
                    DropdownMenu(
                        expanded = showMenu.value,
                        onDismissRequest = { showMenu.value = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("刷新列表") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Refresh,
                                    null,
                                    tint = Color(0xFF2196F3)
                                )
                            },
                            onClick = {
                                showMenu.value = false
                                viewModel.currentPath.value?.let { viewModel.loadFiles(it) }
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("按名称排序") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SortByAlpha,
                                    contentDescription = null,
                                    tint = Color(0xFF9C27B0) // 紫色：代表逻辑与组织
                                )
                            },
                            onClick = {
                                showMenu.value = false
                                viewModel.updateSortOrder(ExplorerViewModel.SortOrder.NAME)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("时间排序 (新→旧)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = Color(0xFF00BCD4) // 青色：代表历史回顾
                                )
                            },
                            onClick = {
                                showMenu.value = false
                                viewModel.updateSortOrder(ExplorerViewModel.SortOrder.TIME_DESC)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("时间排序 (旧→新)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Update,
                                    contentDescription = null,
                                    tint = Color(0xFF009688) // 深青色：代表更新顺序
                                )
                            },
                            onClick = {
                                showMenu.value = false
                                viewModel.updateSortOrder(ExplorerViewModel.SortOrder.TIME_ASC)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("从相册导入图片") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50) // 绿色：代表新增与成功
                                )
                            },
                            onClick = {
                                showMenu.value = false
                                imagePickerLauncher.launch("image/*")
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("退出登录") },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.ExitToApp,
                                    null,
                                    tint = Color(0xFFF44336)
                                )
                            },
                            onClick = {
                                showMenu.value = false
                                onLogout()
                            }
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            // 仅在非选中模式下显示悬浮球
            if (selectedFile.value == null) {
                FloatingActionButton(
                    onClick = { showDialog.value = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, "新建")
                }
            }
        },
        bottomBar = {
            // 选中文件时，滑入底部操作栏
            if (selectedFile.value != null) {
                AnimatedVisibility(
                    visible = selectedFile.value != null,
                    enter = expandVertically(expandFrom = Alignment.Bottom),
                    exit = shrinkVertically(shrinkTowards = Alignment.Bottom)
                ) {
                    BottomAppBar(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ActionIconButton(Icons.Default.Delete, "删除") {
                                selectedFile.value?.let { viewModel.deleteItem(it) }
                                selectedFile.value = null
                            }
                            ActionIconButton(Icons.Default.Info, "详情") {
                                showDetailsSheet.value = true
                            }
                            ActionIconButton(Icons.Default.ContentCut, "剪切") {
                                selectedFile.value?.let {
                                    viewModel.setClipboard(
                                        it,
                                        ExplorerViewModel.TransferMode.CUT
                                    )
                                }
                                selectedFile.value = null
                            }
                            ActionIconButton(Icons.Default.ContentCopy, "复制") {
                                selectedFile.value?.let {
                                    viewModel.setClipboard(
                                        it,
                                        ExplorerViewModel.TransferMode.COPY
                                    )
                                }
                                selectedFile.value = null
                            }
                            ActionIconButton(
                                Icons.Default.Edit,
                                "重命名"
                            ) { showRenameDialog.value = true }
                            ActionIconButton(Icons.Default.Cancel, "取消") {
                                selectedFile.value = null
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        // --- 核心内容列表区 ---
        Column(modifier = Modifier.padding(paddingValues)) {
            if (fileList.isEmpty()) {
                // 列表为空时的空状态视图
                EmptyStateView()
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // key={it.path} 提高列表重组性能
                    items(fileList, key = { it.path }) { file ->
                        val isSelected = selectedFile.value?.path == file.path
                        // animateItem 实现平滑的项目移动和增删动画
                        Box(modifier = Modifier.animateItem()) {
                            FileRow(
                                file = file,
                                isSelected = isSelected,
                                onClick = {
                                    if (selectedFile.value != null) {
                                        selectedFile.value = null // 选中模式下点击取消
                                    } else {
                                        if (file.isDirectory) viewModel.loadFiles(File(file.path)) // 文件夹跳转
                                        else viewModel.openFile(context, file) // 文件打开
                                    }
                                },
                                onLongClick = { selectedFile.value = file } // 长按进入选中模式
                            )
                        }
                    }
                }
            }
        }

        // --- 对话框组件挂载 ---

        // 新建文件对话框
        if (showDialog.value) {
            AddFileItemDialog(
                onConfirm = { fileName ->
                    showDialog.value = false
                    viewModel.createItem(fileName)
                },
                onDismiss = { showDialog.value = false }
            )
        }

        // 重命名对话框
        if (showRenameDialog.value && selectedFile.value != null) {
            RenameFileName(
                fileName = selectedFile.value?.name ?: "",
                onConfirm = { fileName ->
                    selectedFile.value?.let { viewModel.renameItem(it, fileName) }
                    showRenameDialog.value = false
                    selectedFile.value = null // 重命名成功后释放选中
                },
                onDismiss = { showRenameDialog.value = false }
            )
        }

        // 详情面板
        if (showDetailsSheet.value && selectedFile.value != null) {
            FileDetailsSheet(
                file = selectedFile.value!!,
                onDismiss = { showDetailsSheet.value = false }
            )
        }
    }
}