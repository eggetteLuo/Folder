package com.eggetteluo.folder.ui.features.explorer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eggetteluo.folder.model.FileIconTheme
import com.eggetteluo.folder.model.FileItem
import com.eggetteluo.folder.util.formatFileDate
import com.eggetteluo.folder.util.formatFileSize
import com.eggetteluo.folder.util.getFolderItemCount

/**
 * 文件列表中的单行项目组件
 * 负责展示文件的图标、名称、详细信息以及选中状态的视觉反馈。
 *
 * @param file 对应的文件数据模型
 * @param isSelected 当前行是否处于被选中状态
 * @param onClick 普通点击事件（用于打开文件或进入目录）
 * @param onLongClick 长按点击事件（用于进入选中模式）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRow(
    file: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // 根据文件后缀获取图标主题
    val theme = getFileTheme(file)

    // 计算背景色：选中时显示半透明的主题容器色
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        Color.Transparent
    }

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        leadingContent = {
            // 左侧显示文件类型图标
            Icon(
                imageVector = theme.icon,
                contentDescription = null,
                tint = theme.color,
                modifier = Modifier.size(40.dp)
            )
        },
        headlineContent = {
            // 中间显示文件名，超出部分省略
            Text(
                text = file.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified
            )
        },
        supportingContent = {
            // 显示辅助信息：日期 | 文件大小/项目数
            val dateStr = formatFileDate(file.createdAt)
            val extraInfo = if (file.isDirectory) {
                "${getFolderItemCount(file.path)}项"
            } else {
                formatFileSize(file.size)
            }

            Text(
                text = "$dateStr  |  $extraInfo",
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color.Gray
            )
        },
        trailingContent = {
            // 右侧状态展示：选中时显示勾选框，未选中时文件夹显示前进箭头
            if (file.isDirectory && !isSelected) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFFD1D1D1),
                    modifier = Modifier.size(20.dp)
                )
            } else if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已选中",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        // 使 ListItem 容器背景透明，以展示 Modifier.background 设置的颜色
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

/**
 * 根据文件扩展名返回对应的图标和颜色方案
 * 实现了不同类型文件（图片、视频、文档、代码等）的视觉区分。
 */
fun getFileTheme(file: FileItem): FileIconTheme {
    val ext = file.extension.lowercase()
    return when {
        file.isDirectory -> FileIconTheme(Icons.Default.Folder, Color(0xFFFFCA28)) // 经典文件夹黄

        // 媒体文件
        ext in listOf("jpg", "jpeg", "png", "webp", "gif") -> FileIconTheme(
            Icons.Default.Image,
            Color(0xFF4CAF50)
        )

        ext in listOf("mp4", "mkv", "avi", "mov") -> FileIconTheme(
            Icons.Default.VideoFile,
            Color(0xFF2196F3)
        )

        ext in listOf("mp3", "wav", "flac", "m4a", "ogg") -> FileIconTheme(
            Icons.Default.AudioFile,
            Color(0xFFE91E63)
        )

        // 文档与工具
        ext == "pdf" -> FileIconTheme(Icons.Default.PictureAsPdf, Color(0xFFF44336))
        ext == "apk" -> FileIconTheme(Icons.Default.Android, Color(0xFF3DDC84))
        ext in listOf("zip", "rar", "7z", "tar", "gz") -> FileIconTheme(
            Icons.Default.Inventory2,
            Color(0xFFFB8C00)
        )

        // 开发相关
        ext in listOf(
            "html",
            "xml",
            "js",
            "json",
            "php",
            "py",
            "cpp",
            "kt"
        ) -> FileIconTheme(Icons.Default.Code, Color(0xFF00BCD4))

        // 办公文档
        ext in listOf("xls", "xlsx", "csv") -> FileIconTheme(
            Icons.Default.TableChart,
            Color(0xFF2E7D32)
        )

        ext in listOf("txt", "doc", "docx", "md") -> FileIconTheme(
            Icons.AutoMirrored.Filled.Article,
            Color(0xFF9C27B0)
        )

        else -> FileIconTheme(Icons.Default.Description, Color.Gray)
    }
}

/**
 * 底部操作栏中的功能按钮（如：删除、详情、重命名）
 * 包含一个图标和下方的文字标签。
 */
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

/**
 * 文件详情模态底部抽屉
 * 点击“详情”后弹出，展示文件完整的路径、读写权限、大小及时间戳信息。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailsSheet(file: FileItem, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "文件详情",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            DetailRow(label = "名称", value = file.name)
            DetailRow(
                label = "类型",
                value = if (file.isDirectory) "文件夹" else "${file.extension.uppercase()} 文件"
            )
            DetailRow(label = "路径", value = file.path)

            if (!file.isDirectory) {
                DetailRow(label = "大小", value = formatFileSize(file.size))
            }

            DetailRow(label = "创建时间", value = formatFileDate(file.createdAt))
            DetailRow(label = "修改时间", value = formatFileDate(file.lastModified))

            // 拼接权限字符串
            val permissions = buildString {
                if (file.canRead) append("可读 ")
                if (file.canWrite) append("可写 ")
                if (file.isHidden) append("(隐藏)")
            }
            DetailRow(label = "属性", value = permissions)
        }
    }
}

/**
 * 详情页中的单行数据组件（标签：值）
 */
@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * 列表为空时的占位视图
 */
@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.LightGray
        )
        Spacer(Modifier.height(16.dp))
        Text("文件夹为空", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
    }
}

/**
 * 创建新项目的输入对话框
 * @param onConfirm 点击确定后的逻辑回调，传递输入的文件名
 * @param onDismiss 点击取消或关闭对话框的回调
 */
@Composable
fun AddFileItemDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val newFolderName = remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onDismiss() },
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
                    onConfirm(newFolderName.value)
                    newFolderName.value = ""
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("取消")
            }
        }
    )
}

/**
 * 文件重命名的输入对话框
 * @param fileName 初始文件名（默认填入输入框中）
 * @param onConfirm 点击确定后的逻辑回调，传递新文件名
 */
@Composable
fun RenameFileName(fileName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val renameTextFieldValue = remember { mutableStateOf(fileName) }
    AlertDialog(
        onDismissRequest = { onDismiss() },
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
                    if (renameTextFieldValue.value.isNotBlank()) {
                        onConfirm(renameTextFieldValue.value)
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("取消")
            }
        }
    )
}