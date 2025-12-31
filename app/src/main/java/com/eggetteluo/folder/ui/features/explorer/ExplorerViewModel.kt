package com.eggetteluo.folder.ui.features.explorer

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import com.eggetteluo.folder.model.FileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class ExplorerViewModel : ViewModel() {
    private val _fileList = MutableStateFlow<List<FileItem>>(emptyList())
    val fileList: StateFlow<List<FileItem>> = _fileList

    private val _currentPath = MutableStateFlow<File?>(null)
    val currentPath: StateFlow<File?> = _currentPath

    private var userRoot: File? = null

    fun initUserSpace(userId: String) {
        val root = File("/storage/emulated/0/MyFileManager/$userId")
        if (!root.exists()) root.mkdirs()

        userRoot = root
        loadFiles(root)
    }

    fun isRoot(): Boolean {
        return _currentPath.value?.absolutePath == userRoot?.absolutePath
    }

    fun loadFiles(directory: File) {
        val files = directory.listFiles()?.map {
            FileItem(it.name, it.absolutePath, it.isDirectory, it.length(), it.lastModified())
        }?.sortedWith(
            compareByDescending<FileItem> { it.isDirectory } // 文件夹(true)排在文件(false)前面
                .thenBy { it.name.lowercase() }              // 然后按名称不区分大小写排序
        ) ?: emptyList()

        _fileList.value = files
        _currentPath.value = directory
    }

    fun navigateBack(): Boolean {
        val current = _currentPath.value ?: return false
        val root = userRoot ?: return false

        // 1. 如果当前路径的绝对路径已经等于根目录的绝对路径，则拦截，不进行回退
        if (current.absolutePath == root.absolutePath) {
            return false
        }

        // 2. 否则获取父级目录并跳转
        val parent = current.parentFile
        if (parent != null) {
            loadFiles(parent)
            return true
        }

        return false
    }

    /**
     * 创建项目：根据是否有后缀名自动判断是文件还是文件夹
     */
    fun createItem(name: String) {
        val currentDir = _currentPath.value ?: return
        val targetFile = File(currentDir, name)

        if (targetFile.exists()) {
            // 这里可以根据需要添加“文件已存在”的提示逻辑
            return
        }

        try {
            val success = if (name.contains(".")) {
                // 有扩展名 -> 创建文件
                targetFile.createNewFile()
            } else {
                // 无扩展名 -> 创建文件夹
                targetFile.mkdir()
            }

            if (success) {
                // 关键：创建成功后，重新加载当前目录以刷新 UI
                loadFiles(currentDir)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("xiaoluo", "文件创建失败: ${e.message}")
            // 实际开发中建议通过另一个 StateFlow 发送错误消息给 UI
        }
    }

    fun openFile(context: Context, fileItem: FileItem) {
        val file = File(fileItem.path)
        if (!file.exists()) return

        // 1. 获取文件的扩展名
        val extension = MimeTypeMap.getFileExtensionFromUrl(file.absolutePath)
        // 2. 根据扩展名获取 MIME 类型（例如 .txt -> text/plain）
        val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())

        // 3. 使用 FileProvider 生成安全的 URI
        // 注意：这里的 "${context.packageName}.fileprovider" 必须与 Manifest 中一致
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        // 4. 创建并启动 Intent
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, type)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // 授予临时读取权限
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // 如果系统没有能打开该类型文件的应用，可以在这里处理报错
            e.printStackTrace()
        }
    }
}