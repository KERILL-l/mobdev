package io.github.mobdev.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.mobdev.R
import io.github.mobdev.data.AuthEvents
import io.github.mobdev.data.ChatMessage
import io.github.mobdev.data.ChatRepository
import io.github.mobdev.data.NetworkMonitor
import io.github.mobdev.data.SessionManager
import io.github.mobdev.data.db.ChatDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val channels: List<String> = emptyList(),
    val isLoadingChannels: Boolean = false,
    val channelsError: String? = null,
    val selectedChannel: String? = null,
    val messagesByChannel: Map<String, List<ChatMessage>> = emptyMap(),
    val isLoadingMessages: Boolean = false,
    val isSendingMessage: Boolean = false,
    val messagesError: String? = null,
    val fullScreenImageUrl: String? = null,
    val isLoggedOut: Boolean = false,
    val isOnline: Boolean = true,
) {
    val currentMessages: List<ChatMessage>
        get() = selectedChannel?.let { messagesByChannel[it] }.orEmpty()
}

class MainViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: ChatRepository = ChatRepository(
        sessionManager = SessionManager(application),
        chatDao = ChatDatabase.getInstance(application).chatDao(),
        networkMonitor = NetworkMonitor(application),
    ),
    private val networkMonitor: NetworkMonitor = repository.networkMonitor ?: NetworkMonitor(application),
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState(isOnline = networkMonitor.isOnline.value))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val currentUsername: String
        get() = repository.sessionManager.username.orEmpty()

    init {
        observeUnauthorized()
        observeNetwork()
        viewModelScope.launch {
            if (networkMonitor.isOnline.value) {
                repository.syncPendingMessages()
            }
        }
        loadChannels(force = false)
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
        viewModelScope.launch {
            networkMonitor.onConnectedFlow.collect {
                // Connection restored!
                // 1. Sync any pending offline messages
                repository.syncPendingMessages()
                // 2. Refresh channels & current chat
                loadChannels(force = true)
                val currentChannel = _uiState.value.selectedChannel
                if (!currentChannel.isNullOrBlank()) {
                    loadMessages(channel = currentChannel, force = true)
                }
            }
        }
    }

    private fun observeUnauthorized() {
        viewModelScope.launch {
            AuthEvents.unauthorizedFlow.collect {
                logout()
            }
        }
    }

    fun loadChannels(force: Boolean = false) {
        if (!force && _uiState.value.channels.isNotEmpty()) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChannels = true, channelsError = null) }
            val result = repository.getChannels()
            result.onSuccess { channels ->
                _uiState.update {
                    it.copy(
                        channels = channels,
                        isLoadingChannels = false,
                        channelsError = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingChannels = false,
                        channelsError = error.localizedMessage
                            ?: getApplication<Application>().getString(R.string.network_error),
                    )
                }
            }
        }
    }

    fun selectChannel(channel: String) {
        if (_uiState.value.selectedChannel == channel) return
        _uiState.update {
            it.copy(
                selectedChannel = channel,
                messagesError = null,
            )
        }
        loadMessages(channel = channel, force = false)
    }

    fun clearSelectedChannel() {
        _uiState.update {
            it.copy(selectedChannel = null)
        }
    }

    fun loadMessages(channel: String = _uiState.value.selectedChannel.orEmpty(), force: Boolean = false) {
        if (channel.isBlank()) return
        if (!force && _uiState.value.messagesByChannel.containsKey(channel)) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMessages = true, messagesError = null) }
            val result = repository.getMessages(channel = channel, limit = 20)
            result.onSuccess { newMessages ->
                _uiState.update { state ->
                    val updatedMap = state.messagesByChannel.toMutableMap()
                    updatedMap[channel] = newMessages
                    state.copy(
                        messagesByChannel = updatedMap,
                        isLoadingMessages = false,
                        messagesError = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingMessages = false,
                        messagesError = error.localizedMessage
                            ?: getApplication<Application>().getString(R.string.network_error),
                    )
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val channel = _uiState.value.selectedChannel ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingMessage = true, messagesError = null) }
            val result = repository.sendMessage(channel, trimmed)
            result.onSuccess {
                _uiState.update { it.copy(isSendingMessage = false) }
                loadMessages(channel, force = true)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSendingMessage = false,
                        messagesError = error.localizedMessage
                            ?: getApplication<Application>().getString(R.string.network_error),
                    )
                }
            }
        }
    }

    fun openImage(url: String) {
        _uiState.update { it.copy(fullScreenImageUrl = url) }
    }

    fun closeImage() {
        _uiState.update { it.copy(fullScreenImageUrl = null) }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.update {
                it.copy(
                    isLoggedOut = true,
                    selectedChannel = null,
                    fullScreenImageUrl = null,
                )
            }
        }
    }
}
