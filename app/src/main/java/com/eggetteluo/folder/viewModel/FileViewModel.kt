package com.eggetteluo.folder.viewModel

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eggetteluo.folder.model.FileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class FileViewModel : ViewModel() {
    private val _hasPermission = MutableStateFlow(Environment.isExternalStorageManager())
    val hasPermission: StateFlow<Boolean> = _hasPermission

    private val _fileList = MutableStateFlow<List<FileItem>>(emptyList())
    val fileList: StateFlow<List<FileItem>> = _fileList

    private val _currentPath = MutableStateFlow(Environment.getExternalStorageDirectory())
    val currentPath: StateFlow<File> = _currentPath

    init {
        if (_hasPermission.value) {
            loadFiles(_currentPath.value)
        }
    }

    fun onPermissionGranted() {
        _hasPermission.value = true
        loadFiles(_currentPath.value)
    }

    fun loadFiles(directory: File) {
        viewModelScope.launch {
            if (directory.isDirectory && directory.canRead()) {
                val files = directory.listFiles()?.map {
                    FileItem(
                        name = it.name,
                        path = it.absolutePath,
                        isDirectory = it.isDirectory,
                        size = it.length(),
                        lastModified = it.lastModified()
                    )
                }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()

                _fileList.value = files
                _currentPath.value = directory
            }
        }
    }

    fun navigateBack(): Boolean {
        val parent = _currentPath.value.parentFile
        // 限制不能回退到根目录之外
        if (parent != null && parent.absolutePath.startsWith(Environment.getExternalStorageDirectory().absolutePath)) {
            loadFiles(parent)
            return true
        }
        return false
    }
}