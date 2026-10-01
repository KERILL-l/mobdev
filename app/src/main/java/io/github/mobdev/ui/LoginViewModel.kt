package io.github.mobdev.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.github.mobdev.data.ChatRepository
import io.github.mobdev.data.SessionManager
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val repository = ChatRepository(sessionManager = sessionManager)

    private val _uiState = MutableLiveData(LoginUiState())
    val uiState: LiveData<LoginUiState> = _uiState

    val hasActiveSession: Boolean
        get() = sessionManager.isLoggedIn

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState(errorMessage = "Введите имя пользователя и пароль")
            return
        }

        _uiState.value = LoginUiState(isLoading = true)

        viewModelScope.launch {
            val result = repository.login(username.trim(), password)
            if (result.isSuccess) {
                _uiState.value = LoginUiState(isSuccess = true)
            } else {
                val error = result.exceptionOrNull()?.localizedMessage ?: "Ошибка авторизации"
                _uiState.value = LoginUiState(errorMessage = error)
            }
        }
    }
}
