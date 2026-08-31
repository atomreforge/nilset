package net.atomreforge.nilset.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import net.atomreforge.nilset.data.repository.SessionRepository
import javax.inject.Inject

/**
 * UI 层：登录页 ViewModel。
 *
 * 职责：持有登录表单状态，处理登录事件，把结果通过 [LoginUiState] 单向暴露给 UI。
 * 登录请求通过 [SessionRepository] 发到服务端，本类不依赖任何 Android 视图，可直接做单元测试。
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** 用户名输入变化（事件向上） */
    fun onUsernameChanged(value: String) {
        _uiState.update { it.copy(username = value) }
    }

    /** 密码输入变化（事件向上） */
    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    /** 登录按钮点击：校验输入，把登录动作交给数据层 */
    fun onLoginClicked() {
        val username = _uiState.value.username.trim()
        val password = _uiState.value.password.trim()

        if (username.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(loginMessage = "请输入用户名和密码") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            sessionRepository.login(username, password)
                .onSuccess {
                    _uiState.update {
                        it.copy(isLoading = false, loginMessage = "登录成功，欢迎 $username")
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, loginMessage = "登录失败：${e.message}")
                    }
                }
        }
    }

    /** UI 展示完提示后调用，清空消息防止重复弹 Toast */
    fun onLoginMessageShown() {
        _uiState.update { it.copy(loginMessage = null) }
    }

}

/** 登录页 UI 状态：不可变，UI 只读它来渲染 */
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val loginMessage: String? = null,
    val isLoading: Boolean = false,
)
