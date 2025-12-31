package com.eggetteluo.folder.ui.features.explorer

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eggetteluo.folder.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes

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

    // 在 ExplorerViewModel.kt 中
    fun loadFiles(directory: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val files = directory.listFiles()?.map { file ->
                // 获取创建时间
                val creationTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        val path = Paths.get(file.absolutePath)
                        val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)
                        attributes.creationTime().toMillis()
                    } catch (e: Exception) {
                        file.lastModified() // 如果读取失败，回退到最后修改时间
                    }
                } else {
                    file.lastModified() // 低版本 Android 不支持，默认用最后修改时间
                }

                FileItem(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    size = if (file.isDirectory) 0L else file.length(),
                    lastModified = file.lastModified(),
                    createdAt = creationTime,
                    canRead = file.canRead(),
                    canWrite = file.canWrite(),
                    isHidden = file.isHidden,
                    extension = file.extension
                )
            }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()

            withContext(Dispatchers.Main) {
                _fileList.value = files
                _currentPath.value = directory
            }
        }
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

        val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

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