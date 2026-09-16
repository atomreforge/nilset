package net.atomreforge.nilset.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.atomreforge.nilset.data.repository.SessionRepository
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onUsernameChanged(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun onNicknameChanged(value: String) {
        _uiState.update { it.copy(nickname = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun onRegisterCodeChanged(value: String) {
        _uiState.update { it.copy(registerCode = value, errorMessage = null) }
    }

    fun onRegisterClicked() {
        val state = _uiState.value
        if (state.isLoading) return

        val validationError = RegisterFormValidator.validate(
            username = state.username,
            nickname = state.nickname,
            password = state.password,
            confirmPassword = state.confirmPassword,
            registerCode = state.registerCode,
        )
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            sessionRepository.register(
                username = state.username.trim(),
                nickname = state.nickname.trim(),
                password = state.password,
                registerCode = state.registerCode.trim(),
            ).onSuccess {
                _uiState.update { current ->
                    current.copy(isLoading = false, isRegisterSuccess = true)
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "注册失败，请稍后重试",
                    )
                }
            }
        }
    }
}

data class RegisterUiState(
    val username: String = "",
    val nickname: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val registerCode: String = "",
    val isLoading: Boolean = false,
    val isRegisterSuccess: Boolean = false,
    val errorMessage: String? = null,
)

object RegisterFormValidator {
    private val usernamePattern = Regex("^[A-Za-z0-9]+$")
    private val registerCodePattern = Regex("^[0-9a-fA-F]{1,2048}\\.[0-9a-fA-F]{128}$")

    fun validate(
        username: String,
        nickname: String,
        password: String,
        confirmPassword: String,
        registerCode: String,
    ): String? {
        val normalizedUsername = username.trim()
        val normalizedNickname = nickname.trim()
        val normalizedRegisterCode = registerCode.trim()

        return when {
            normalizedUsername.isEmpty() -> "请输入用户名"
            normalizedUsername.length !in 1..15 || !usernamePattern.matches(normalizedUsername) ->
                "用户名需为1-15位字母或数字"
            normalizedNickname.isEmpty() -> "请输入昵称"
            normalizedNickname.codePointCount(0, normalizedNickname.length) !in 1..15 ->
                "昵称需为1-15个字符"
            password.isEmpty() -> "请输入密码"
            password.codePointCount(0, password.length) !in 6..128 -> "密码需为6-128个字符"
            confirmPassword.isEmpty() -> "请再次输入密码"
            password != confirmPassword -> "两次输入的密码不一致"
            normalizedRegisterCode.isEmpty() -> "请输入注册码"
            !registerCodePattern.matches(normalizedRegisterCode) -> "注册码格式不正确"
            else -> null
        }
    }
}
