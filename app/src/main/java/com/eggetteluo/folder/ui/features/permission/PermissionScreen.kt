package com.eggetteluo.folder.ui.features.permission

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.eggetteluo.folder.util.requestStoragePermission // 导入你的工具函数

@Composable
fun PermissionScreen(onPermissionGranted: () -> Unit) {
    // 获取当前的 Activity
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "需要文件访问权限",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "为了实现多用户文件隔离管理，我们需要“所有文件访问权限”。请在设置中勾选允许访问。",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // 调用你 util 包里的函数
                activity?.let {
                    requestStoragePermission(it) {
                        onPermissionGranted() // 授权成功后的回调
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("立即去授权")
        }
    }
}