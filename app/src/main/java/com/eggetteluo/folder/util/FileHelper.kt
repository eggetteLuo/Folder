package com.eggetteluo.folder.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.eggetteluo.folder.model.FileItem
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes

/**
 * 文件操作工具单例对象
 * 封装了对底层文件系统的物理操作，包括读取、创建、删除、移动和复制等。
 */
object FileHelper {
    private const val TAG = "FileHelper"

    /**
     * 扫描指定目录下的所有文件和子目录
     * * @param directory 目标父目录对象
     * @return 返回封装好的 [FileItem] 列表，若目录不可读或为空则返回空列表
     */
    fun listFiles(directory: File): List<FileItem> {
        return directory.listFiles()?.map { file ->
            val creationTime = getCreationTime(file)
            FileItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                // 文件夹大小通常在文件系统中不直接体现，设为 0L；文件则获取其实际长度
                size = if (file.isDirectory) 0L else file.length(),
                lastModified = file.lastModified(),
                createdAt = creationTime,
                canRead = file.canRead(),
                canWrite = file.canWrite(),
                isHidden = file.isHidden,
                extension = file.extension
            )
        } ?: emptyList()
    }

    /**
     * 获取物理文件的创建时间
     * * 兼容性处理：
     * - Android 8.0 (API 26) 及以上：尝试通过 NIO 访问 [BasicFileAttributes] 获取精确创建时间。
     * - 低版本或获取失败：退而求其次使用最后修改时间 (lastModified)。
     * * @param file 目标文件
     * @return 毫秒级时间戳
     */
    private fun getCreationTime(file: File): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val path = Paths.get(file.absolutePath)
                val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)
                attributes.creationTime().toMillis()
            } catch (_: Exception) {
                file.lastModified()
            }
        } else {
            file.lastModified()
        }
    }

    /**
     * 执行物理删除操作
     * * @param file 要删除的对象。如果是目录，将执行递归删除（包含其下所有子项）。
     * @return 删除成功返回 true，否则返回 false。
     */
    fun delete(file: File): Boolean {
        return if (file.isDirectory) file.deleteRecursively() else file.delete()
    }

    /**
     * 执行粘贴逻辑（支持跨目录复制或移动）
     * * @param sourcePath 源文件的绝对路径
     * @param targetFolder 粘贴的目标父目录
     * @param isMove true 表示移动（剪切粘贴），false 表示复制粘贴
     * @return 操作成功返回 true，发生 I/O 异常返回 false
     */
    fun paste(sourcePath: String, targetFolder: File, isMove: Boolean): Boolean {
        val sourceFile = File(sourcePath)
        val destFile = File(targetFolder, sourceFile.name)
        return try {
            if (isMove) {
                // 物理移动：尝试直接重命名（最快），若失败则需要手动流拷贝（跨分区）
                sourceFile.renameTo(destFile)
            } else {
                // 物理复制：递归拷贝所有子文件及文件夹，覆盖已存在的同名文件
                sourceFile.copyRecursively(destFile, overwrite = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Paste failed", e)
            false
        }
    }

    /**
     * 创建新的文件或文件夹
     * * @param parentDir 目标位置的父目录
     * @param name 新项的名称
     * @param isDirectory 可选明确指定类型。若为 null，则根据名称中是否包含 "." 自动判断。
     * @return 创建成功且无重名项返回 true
     */
    fun createItem(parentDir: File, name: String, isDirectory: Boolean? = null): Boolean {
        if (name.isBlank()) return false

        val targetFile = File(parentDir, name)
        // 健壮性检查：如果该路径已存在，则禁止创建以防误删
        if (targetFile.exists()) return false

        return try {
            val shouldCreateDir = isDirectory ?: !name.contains(".")

            if (shouldCreateDir) {
                targetFile.mkdir()
            } else {
                targetFile.createNewFile()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Create item failed: ${e.message}")
            false
        }
    }

    /**
     * 重命名文件或文件夹
     * * @param file 原物理文件对象
     * @param newName 新的文件名（不包含路径）
     * @return 成功且目标路径无冲突返回 true
     */
    fun rename(file: File, newName: String): Boolean {
        val dest = File(file.parentFile, newName)
        // 确保新名字没有被占用
        return if (!dest.exists()) file.renameTo(dest) else false
    }

    /**
     * 将从 Android ContentResolver 获取的 Uri 输入流保存为物理文件
     * 常用于从系统相册、外部文档选择器导入数据
     * * @param context 上下文用于访问 ContentResolver
     * @param uri 外部数据的 Uri (content://)
     * @param targetFile 要保存到的本地 File 对象
     * @return 拷贝完成返回 true
     */
    fun saveUriStream(context: Context, uri: android.net.Uri, targetFile: File): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    // 高效流拷贝
                    input.copyTo(output)
                    true
                }
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Save Uri failed", e)
            false
        }
    }
}