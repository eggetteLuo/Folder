package com.eggetteluo.folder.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 文件图标主题模型
 * 用于封装文件在列表项中展示的图标向量和颜色方案
 *
 * @property icon 图标的 ImageVector（如 Icons.Default.Folder）
 * @property color 图标的着色（如 Color.Yellow）
 */
data class FileIconTheme(
    val icon: ImageVector,
    val color: Color
)