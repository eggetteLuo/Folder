package com.eggetteluo.folder.ui.features.explorer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eggetteluo.folder.model.FileItem

// 组件 1：单个文件行
@Composable
fun FileRow(file: FileItem, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = { Text(file.name) },
        leadingContent = {
            Icon(
                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                tint = if (file.isDirectory) Color(0xFFFFCA28) else Color.Gray
            )
        }
    )
}

// 组件 2：顶部路径面包屑（可选）
@Composable
fun PathHeader(path: String) {
    Surface(tonalElevation = 4.dp) {
        Text(
            text = path,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}