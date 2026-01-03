package com.eggetteluo.folder.model

data class FileItem(
    val name: String,           // 文件名
    val path: String,           // 文件的真实物理绝对路径
    val isDirectory: Boolean,    // 是否为文件夹
    val size: Long,              // 文件大小 (字节)
    val lastModified: Long,      // 最后修改时间戳
    val createdAt: Long = 0L,    // 创建时间
    val canRead: Boolean,        // 是否可读
    val canWrite: Boolean,       // 是否可写
    val isHidden: Boolean,       // 是否是隐藏文件
    val extension: String = ""   // 后缀名 (如 "jpg", "pdf")
)