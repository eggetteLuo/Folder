package com.eggetteluo.folder.ui.features.explorer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eggetteluo.folder.model.FileItem
import com.eggetteluo.folder.util.formatFileDate
import com.eggetteluo.folder.util.formatFileSize
import com.eggetteluo.folder.util.getFolderItemCount

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRow(
    file: FileItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val theme = getFileTheme(file)

    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick // 绑定长按事件
        ),
        leadingContent = {
            Icon(
                imageVector = theme.icon,
                contentDescription = null,
                tint = theme.color,
                modifier = Modifier.size(40.dp)
            )
        },
        headlineContent = {
            Text(
                text = file.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            // 拼接辅助信息：日期 | (项数 或 大小)
            val dateStr = formatFileDate(file.createdAt)
            val extraInfo = if (file.isDirectory) {
                "${getFolderItemCount(file.path)}项"
            } else {
                formatFileSize(file.size)
            }

            Text(
                text = "$dateStr  |  $extraInfo",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        },
        trailingContent = {
            if (file.isDirectory) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFFD1D1D1),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    )
}

// 定义一个数据类来存放结果
data class FileIconTheme(val icon: ImageVector, val color: Color)

fun getFileTheme(file: FileItem): FileIconTheme {
    val ext = file.extension.lowercase()
    return when {
        file.isDirectory -> FileIconTheme(Icons.Default.Folder, Color(0xFFFFCA28))

        // 图片
        ext in listOf("jpg", "jpeg", "png", "webp", "gif") ->
            FileIconTheme(Icons.Default.Image, Color(0xFF4CAF50))

        // 视频
        ext in listOf("mp4", "mkv", "avi", "mov") ->
            FileIconTheme(Icons.Default.VideoFile, Color(0xFF2196F3))

        // 音频
        ext in listOf("mp3", "wav", "flac", "m4a", "ogg") ->
            FileIconTheme(Icons.Default.AudioFile, Color(0xFFE91E63))

        // PDF
        ext == "pdf" ->
            FileIconTheme(Icons.Default.PictureAsPdf, Color(0xFFF44336))

        // Android 安装包
        ext == "apk" ->
            FileIconTheme(Icons.Default.Android, Color(0xFF3DDC84))

        // 压缩包
        ext in listOf("zip", "rar", "7z", "tar", "gz") ->
            FileIconTheme(Icons.Default.Inventory2, Color(0xFFFB8C00))

        // 代码/网页文件
        ext in listOf("html", "xml", "js", "json", "php", "py", "cpp", "kt") ->
            FileIconTheme(Icons.Default.Code, Color(0xFF00BCD4))

        // 电子书/表格 (区分纯文本)
        ext in listOf("epub", "mobi") ->
            FileIconTheme(Icons.Default.AutoStories, Color(0xFF795548))

        // 表格
        ext in listOf("xls", "xlsx", "csv") ->
            FileIconTheme(Icons.Default.TableChart, Color(0xFF2E7D32))

        // 文档
        ext in listOf("txt", "doc", "docx", "md") ->
            FileIconTheme(Icons.Default.Article, Color(0xFF9C27B0))

        else -> FileIconTheme(Icons.Default.Description, Color.Gray)
    }
}

@Composable
fun ActionIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clickable { onClick() }
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}