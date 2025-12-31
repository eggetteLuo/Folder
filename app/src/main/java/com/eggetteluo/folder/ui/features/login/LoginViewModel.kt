package com.eggetteluo.folder.ui.features.login

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LoginViewModel : ViewModel() {
    // UI 状态：记录用户名输入
    var usernameInput = MutableStateFlow("")

    // 错误信息状态：如果用户名为空或格式不对，显示给用户
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun onUsernameChange(newName: String) {
        usernameInput.value = newName
        _errorMessage.value = null // 输入时清除错误
    }

    // 执行登录逻辑
    fun performLogin(onSuccess: (String) -> Unit) {
        val user = usernameInput.value.trim()

        if (user.isBlank()) {
            _errorMessage.value = "用户名不能为空"
            return
        }

        // 模拟验证：实际开发中这里会去查数据库
        if (user.length < 3) {
            _errorMessage.value = "用户名至少需要3个字符"
            return
        }

        // 验证通过，触发回调通知导航跳转
        onSuccess(user)
    }
}