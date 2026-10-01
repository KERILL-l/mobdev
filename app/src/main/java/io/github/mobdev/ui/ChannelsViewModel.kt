package io.github.mobdev.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.github.mobdev.data.ChatRepository
import io.github.mobdev.data.SessionManager
import kotlinx.coroutines.launch

data class ChannelsUiState(
    val isLoading: Boolean = false,
    val channels: List<String> = emptyList(),
    val errorMessage: String? = null,
    val isLoggedOut: Boolean = false,
)

class ChannelsViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val repository = ChatRepository(sessionManager = sessionManager)

    private val _uiState = MutableLiveData(ChannelsUiState())
    val uiState: LiveData<ChannelsUiState> = _uiState

    val currentUsername: String
        get() = sessionManager.username ?: ""

    init {
        loadChannels()
    }

    fun loadChannels() {
        _uiState.value = _uiState.value?.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.getChannels()
            if (result.isSuccess) {
                _uiState.value = ChannelsUiState(
                    channels = result.getOrDefault(emptyList()),
                    isLoading = false,
                )
            } else {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Ошибка загрузки каналов",
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = ChannelsUiState(isLoggedOut = true)
        }
    }
}
