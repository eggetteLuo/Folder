package com.eggetteluo.folder.ui.features.explorer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eggetteluo.folder.model.FileItem
import com.eggetteluo.folder.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * 文件资源管理器的核心 ViewModel
 * 负责维护文件列表状态、路径导航、剪贴板逻辑以及与物理文件系统的交互
 */
class ExplorerViewModel : ViewModel() {

    // --- 枚举定义 ---

    /** 排序方式：按名称、按时间倒序、按时间正序 */
    enum class SortOrder { NAME, TIME_DESC, TIME_ASC }

    /** 文件传输模式：复制、剪切、无 */
    enum class TransferMode { COPY, CUT, NONE }

    // --- 状态流 (StateFlow) ---

    // 当前目录下的文件列表数据
    private val _fileList = MutableStateFlow<List<FileItem>>(emptyList())
    val fileList: StateFlow<List<FileItem>> = _fileList

    // 当前所处的物理目录路径
    private val _currentPath = MutableStateFlow<File?>(null)
    val currentPath: StateFlow<File?> = _currentPath

    // 剪贴板中持有的文件对象
    private val _clipboardFile = MutableStateFlow<FileItem?>(null)
    val clipboardFile: StateFlow<FileItem?> = _clipboardFile

    // 当前剪贴板的操作模式（复制或剪切）
    private val _transferMode = MutableStateFlow(TransferMode.NONE)
    val transferMode: StateFlow<TransferMode> = _transferMode

    // --- 事件流 (SharedFlow) ---

    // 一次性错误消息通知流，用于 UI 弹出 Snackbar 或 Toast
    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()

    // 内部持有的当前排序规则
    private val _sortOrder = MutableStateFlow(SortOrder.NAME)

    // 用户的根目录（在该应用空间内的最高级目录）
    private var userRoot: File? = null

    // --- 核心业务方法 ---

    /**
     * 初始化用户的私人文件空间
     * @param userId 用户唯一标识，用于隔离不同用户的文件
     */
    fun initUserSpace(userId: String) {
        if (userRoot != null) return // 避免重复初始化
        val root = File("/storage/emulated/0/MyFileManager/$userId")
        if (!root.exists()) root.mkdirs() // 如果文件夹不存在则递归创建
        userRoot = root
        loadFiles(root)
    }

    /**
     * 判断当前是否处于用户空间的根目录
     * @return true 表示已无法再向后退
     */
    fun isRoot(): Boolean {
        return _currentPath.value?.absolutePath == userRoot?.absolutePath
    }

    /**
     * 向上一级目录导航
     * @return true 表示成功返回上一级，false 表示已在根目录
     */
    fun navigateBack(): Boolean {
        val current = _currentPath.value ?: return false
        val root = userRoot ?: return false

        // 如果当前已经是根目录，拒绝回退请求
        if (current.absolutePath == root.absolutePath) {
            return false
        }

        val parent = current.parentFile
        if (parent != null) {
            loadFiles(parent)
            return true
        }
        return false
    }

    /**
     * 从物理磁盘加载指定目录的文件列表
     * @param directory 目标目录，默认为当前目录
     */
    fun loadFiles(directory: File? = _currentPath.value) {
        if (directory == null) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                // 调用工具类扫描磁盘
                FileHelper.listFiles(directory)
            }.onSuccess { files ->
                // 应用当前的排序算法
                val sorted = sortFiles(files, _sortOrder.value)
                _fileList.value = sorted
                _currentPath.value = directory
            }.onFailure {
                _errorMessage.emit("无法读取目录：${it.message}")
            }
        }
    }

    /**
     * 在当前目录下创建新项目
     * @param name 新建的文件或文件夹名称（FileHelper 根据名称后缀自动判断类型）
     */
    fun createItem(name: String) {
        val currentDir = _currentPath.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val success = FileHelper.createItem(currentDir, name)
            if (success) {
                loadFiles(currentDir) // 创建成功后自动刷新列表
            } else {
                _errorMessage.emit("创建失败：项目已存在或名称非法")
            }
        }
    }

    /**
     * 调用 Android 系统能力尝试打开文件
     * @param context 上下文用于启动 Activity
     * @param fileItem 要打开的目标文件模型
     */
    fun openFile(context: Context, fileItem: FileItem) {
        val file = File(fileItem.path)

        if (!file.exists()) {
            viewModelScope.launch { _errorMessage.emit("文件不存在") }
            return
        }

        try {
            // 获取文件的后缀并解析 MimeType（媒体类型）
            val extension = file.extension.lowercase()
            var type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

            // 如果无法识别类型，使用通用的二进制流类型
            if (type == null) { type = "*/*" }

            // 使用 FileProvider 生成安全 Uri，避免 7.0+ 后的 FileUriExposedException
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // 构建隐式 Intent 启动第三方应用（如相册、视频播放器）
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, type)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // 授予临时读取权限
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            viewModelScope.launch {
                _errorMessage.emit("没有找到可以打开此文件的应用")
            }
        }
    }

    /**
     * 更新当前界面的排序方式
     * @param order 选中的排序枚举
     */
    fun updateSortOrder(order: SortOrder) {
        _sortOrder.value = order
        // 仅对当前内存中的列表进行重新排序，无需重新扫描磁盘（性能优化）
        _fileList.value = sortFiles(_fileList.value, order)
    }

    /**
     * 删除选中的项目
     * @param fileItem 要删除的文件或文件夹模型
     */
    fun deleteItem(fileItem: FileItem) {
        viewModelScope.launch(Dispatchers.IO) {
            if (FileHelper.delete(File(fileItem.path))) {
                loadFiles() // 删除成功后刷新界面
            } else {
                _errorMessage.emit("删除失败：权限不足或文件被占用")
            }
        }
    }

    /**
     * 重命名选中的项目
     * @param fileItem 目标文件
     * @param newName 新的名字
     */
    fun renameItem(fileItem: FileItem, newName: String) {
        val currentFile = File(fileItem.path)

        viewModelScope.launch(Dispatchers.IO) {
            val success = FileHelper.rename(currentFile, newName)
            if (success) {
                loadFiles()
            } else {
                _errorMessage.emit("重命名失败：名称冲突或无权限")
            }
        }
    }

    /**
     * 将文件标记为等待传输状态
     * @param file 要操作的文件
     * @param mode 操作模式（复制或剪切）
     */
    fun setClipboard(file: FileItem, mode: TransferMode) {
        _clipboardFile.value = file
        _transferMode.value = mode
    }

    /**
     * 在目标目录下执行粘贴操作
     * @param targetFolder 目标目录
     */
    fun paste(targetFolder: File) {
        val source = _clipboardFile.value ?: return
        val isMove = _transferMode.value == TransferMode.CUT

        viewModelScope.launch(Dispatchers.IO) {
            // 调用 FileHelper 执行真正的 IO 操作（Copy 或 Move）
            val success = FileHelper.paste(source.path, targetFolder, isMove)
            if (success) {
                // 操作成功后清空剪贴板并刷新
                _clipboardFile.value = null
                _transferMode.value = TransferMode.NONE
                loadFiles(targetFolder)
            } else {
                _errorMessage.emit("粘贴失败：目标位置已存在同名项目")
            }
        }
    }

    /**
     * 将从外部（如系统相册）选中的图片保存到当前目录
     * @param context 环境上下文
     * @param uri 图片的 Uri
     */
    fun saveImageToCurrentDir(context: Context, uri: Uri) {
        val currentDir = _currentPath.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // 生成带时间戳的文件名，避免冲突
            val fileName = "IMG_${System.currentTimeMillis()}.jpg"
            val target = File(currentDir, fileName)

            // 将 Uri 输入流拷贝到本地目标文件
            if (FileHelper.saveUriStream(context, uri, target)) {
                loadFiles()
            } else {
                _errorMessage.emit("保存图片失败")
            }
        }
    }

    /**
     * 内部私有排序逻辑
     * 文件夹始终排在最前面，然后根据指定规则排序
     */
    private fun sortFiles(files: List<FileItem>, order: SortOrder): List<FileItem> {
        return when (order) {
            // 按名称：文件夹优先 -> 名称 A-Z
            SortOrder.NAME -> files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            // 按时间倒序：文件夹优先 -> 最新创建在前
            SortOrder.TIME_DESC -> files.sortedWith(compareBy({ !it.isDirectory }, { -it.createdAt }))
            // 按时间正序：文件夹优先 -> 最老创建在前
            SortOrder.TIME_ASC -> files.sortedWith(compareBy({ !it.isDirectory }, { it.createdAt }))
        }
    }
}