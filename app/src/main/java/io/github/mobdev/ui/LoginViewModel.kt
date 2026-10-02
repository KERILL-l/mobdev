package io.github.mobdev.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.mobdev.R
import io.github.mobdev.data.ChatRepository
import io.github.mobdev.data.NetworkMonitor
import io.github.mobdev.data.SessionManager
import io.github.mobdev.data.db.ChatDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
)

class LoginViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: ChatRepository = ChatRepository(
        sessionManager = SessionManager(application),
        chatDao = ChatDatabase.getInstance(application).chatDao(),
        networkMonitor = NetworkMonitor(application),
    ),
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val hasActiveSession: Boolean
        get() = repository.sessionManager.isLoggedIn

    fun login(username: String, password: String) {
        val trimmedUser = username.trim()
        val trimmedPass = password.trim()
        val app = getApplication<Application>()

        if (trimmedUser.isBlank() || trimmedPass.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = app.getString(R.string.empty_credentials))
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.login(trimmedUser, trimmedPass)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isSuccess = true, errorMessage = null) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = app.getString(R.string.error_invalid_credentials),
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
