package com.eggetteluo.folder.ui.features.explorer

import android.os.Environment
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
        // 这里的路径逻辑可以封装到 data 层
        val root = File("/storage/emulated/0/MyFileManager/$userId")
        if (!root.exists()) root.mkdirs()

        userRoot = root
        loadFiles(root)
    }

    fun loadFiles(directory: File) {
        val files = directory.listFiles()?.map {
            FileItem(it.name, it.absolutePath, it.isDirectory, it.length(), it.lastModified())
        } ?: emptyList()
        _fileList.value = files
        _currentPath.value = directory
    }

    fun navigateBack(): Boolean {
        val parent = _currentPath.value?.parentFile
        // 限制不能回退到根目录之外
        if (parent != null && parent.absolutePath.startsWith(Environment.getExternalStorageDirectory().absolutePath)) {
            loadFiles(parent)
            return true
        }
        return false
    }
}