package com.eggetteluo.folder.ui.features.login

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("user_creds", Context.MODE_PRIVATE)

    private val _usernameInput = MutableStateFlow("")
    val usernameInput: StateFlow<String> = _usernameInput

    private val _passwordInput = MutableStateFlow("")
    val passwordInput: StateFlow<String> = _passwordInput

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun onUsernameChange(input: String) { _usernameInput.value = input }
    fun onPasswordChange(input: String) { _passwordInput.value = input }

    fun performLogin(onSuccess: (String) -> Unit) {
        val user = _usernameInput.value
        val pass = _passwordInput.value

        if (user.isBlank() || pass.isBlank()) {
            _errorMessage.value = "用户名和密码不能为空"
            return
        }

        val savedPass = prefs.getString("pwd_$user", null)

        if (savedPass == null) {
            // 初次登录，自动注册密码
            prefs.edit().putString("pwd_$user", pass).apply()
            onSuccess(user)
        } else {
            // 验证密码
            if (savedPass == pass) {
                _errorMessage.value = null
                onSuccess(user)
            } else {
                _errorMessage.value = "密码错误，请重新输入"
            }
        }
    }
}