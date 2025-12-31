package com.eggetteluo.folder

import android.Manifest
import android.os.Bundle
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.eggetteluo.folder.model.FileItem
import com.eggetteluo.folder.ui.navigation.AppNavGraph
import com.eggetteluo.folder.ui.theme.FolderTheme
import com.eggetteluo.folder.viewModel.FileViewModel
import com.permissionx.guolindev.PermissionX
import java.io.File

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FolderTheme {
                FolderTheme {
                    val navController = rememberNavController()
                    Scaffold { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            AppNavGraph(navController = navController)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileExplorerScreen(viewModel: FileViewModel) {
    val fileList by viewModel.fileList.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()

    // 处理安卓物理返回键：如果不在根目录，点击返回键则跳回上一级
    BackHandler(enabled = currentPath.absolutePath != Environment.getExternalStorageDirectory().absolutePath) {
        viewModel.navigateBack()
    }

    Column {
        Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "当前路径: ${currentPath.absolutePath}",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }

        if (fileList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("文件夹为空", color = Color.Gray)
            }
        } else {
            LazyColumn {
                items(fileList) { file ->
                    FileRow(file) {
                        if (file.isDirectory) {
                            viewModel.loadFiles(File(file.path))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileRow(file: FileItem, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = { Text(file.name) },
        supportingContent = {
            if (!file.isDirectory) {
                Text("${file.size / 1024} KB")
            }
        },
        leadingContent = {
            Icon(
                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                tint = if (file.isDirectory) Color(0xFFFFCA28) else Color.Gray
            )
        }
    )
}

@Composable
fun PermissionGuideScreen(onButtonClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("需要“所有文件访问权限”才能管理文件")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onButtonClick) {
            Text("去授权")
        }
    }
}

fun requestStoragePermission(activity: FragmentActivity, onPermissionGranted: () -> Unit) {
    PermissionX.init(activity)
        .permissions(Manifest.permission.MANAGE_EXTERNAL_STORAGE) // 请求所有文件访问权限
        .onExplainRequestReason { scope, deniedList ->
            scope.showRequestReasonDialog(
                deniedList,
                "文件管理器需要访问所有文件以进行管理，请在设置中允许权限",
                "确定", "取消"
            )
        }
        .onForwardToSettings { scope, deniedList ->
            scope.showForwardToSettingsDialog(
                deniedList,
                "您需要去设置界面手动开启文件访问权限",
                "去设置", "取消"
            )
        }
        .request { allGranted, _, _ ->
            if (allGranted) {
                onPermissionGranted() // 权限获取成功，去刷新文件列表
            } else {
                // 处理权限被拒绝的情况，比如弹一个 Toast
            }
        }
}