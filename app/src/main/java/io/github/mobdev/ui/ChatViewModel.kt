package io.github.mobdev.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.github.mobdev.data.ChatMessage
import io.github.mobdev.data.ChatRepository
import io.github.mobdev.data.SessionManager
import kotlinx.coroutines.launch

data class ChatUiState(
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val errorMessage: String? = null,
    val messageSentSuccess: Boolean = false,
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val repository = ChatRepository(sessionManager = sessionManager)

    private val _uiState = MutableLiveData(ChatUiState())
    val uiState: LiveData<ChatUiState> = _uiState

    val currentUsername: String
        get() = sessionManager.username ?: ""

    private var activeChannel: String = ""

    fun initChannel(channel: String) {
        if (activeChannel != channel) {
            activeChannel = channel
            loadMessages()
        }
    }

    fun loadMessages() {
        if (activeChannel.isBlank()) return
        _uiState.value = _uiState.value?.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = repository.getMessages(channel = activeChannel)
            if (result.isSuccess) {
                // Messages are received in reverse or standard order. We sort them by ID ascending for a natural chat feed.
                val sorted = result.getOrDefault(emptyList()).sortedBy { it.id }
                _uiState.value = ChatUiState(
                    messages = sorted,
                    isLoading = false,
                )
            } else {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Ошибка загрузки сообщений",
                )
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || activeChannel.isBlank()) return
        _uiState.value = _uiState.value?.copy(isSending = true, errorMessage = null)

        viewModelScope.launch {
            val result = repository.sendMessage(channel = activeChannel, text = text.trim())
            if (result.isSuccess) {
                // Refresh messages after sending
                loadMessages()
            } else {
                _uiState.value = _uiState.value?.copy(
                    isSending = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Не удалось отправить сообщение",
                )
            }
        }
    }
}
