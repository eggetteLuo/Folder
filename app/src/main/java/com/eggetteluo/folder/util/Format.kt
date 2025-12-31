package com.eggetteluo.folder.util

import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

// 格式化时间戳为 2025/05/03 格式
fun formatFileDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// 格式化大小 (B, KB, MB...)
fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
}

// 获取文件夹下的项数（仅对文件夹有效）
fun getFolderItemCount(path: String): Int {
    return File(path).listFiles()?.size ?: 0
}