package com.eggetteluo.folder.ui.features.explorer

import android.text.format.Formatter.formatFileSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eggetteluo.folder.model.FileItem

// 组件 1：单个文件行
@Composable
fun FileRow(file: FileItem, onClick: () -> Unit) {
    val (icon, tint) = when {
        file.isDirectory -> Icons.Default.Folder to Color(0xFFFFCA28) // 橙黄色文件夹
        file.name.endsWith(".jpg", true) || file.name.endsWith(".png", true) ->
            Icons.Default.Image to Color(0xFF4CAF50) // 绿色图片
        file.name.endsWith(".mp4", true) || file.name.endsWith(".mkv", true) ->
            Icons.Default.VideoFile to Color(0xFF2196F3) // 蓝色视频
        file.name.endsWith(".mp3", true) || file.name.endsWith(".wav", true) ->
            Icons.Default.AudioFile to Color(0xFFE91E63) // 粉色音频
        file.name.endsWith(".pdf", true) ->
            Icons.Default.PictureAsPdf to Color(0xFFF44336) // 红色 PDF
        else -> Icons.Default.Description to Color.Gray // 默认灰色文件
    }

    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = {
            Text(
                text = file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint
            )
        },
        trailingContent = {
            if (file.isDirectory) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "进入",
                    tint = Color.LightGray
                )
            }
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