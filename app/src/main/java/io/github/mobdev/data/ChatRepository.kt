package io.github.mobdev.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatRepository(
    private val api: ChatApi = ApiClient.api,
    val sessionManager: SessionManager,
) {

    suspend fun login(name: String, pwd: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val responseBody = api.login(LoginRequest(name, pwd))
            val rawToken = responseBody.string().trim()
            val token = rawToken.removeSurrounding("\"").trim()
            if (token.isBlank()) {
                throw IllegalStateException("Empty token received")
            }
            sessionManager.saveSession(username = name, token = token)
            token
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = sessionManager.token
            if (!token.isNullOrBlank()) {
                try {
                    api.logout(token)
                } catch (e: Exception) {
                    // Ignore logout network errors, still clear session locally
                }
            }
            sessionManager.clear()
        }
    }

    suspend fun getChannels(): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val token = sessionManager.token ?: throw IllegalStateException("Not authenticated")
            api.getChannels(token)
        }
    }

    suspend fun getMessages(
        channel: String,
        limit: Int = 30,
        lastKnownId: Long? = null,
    ): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        runCatching {
            val token = sessionManager.token ?: throw IllegalStateException("Not authenticated")
            api.getChannelMessages(
                token = token,
                channel = channel,
                limit = limit,
                lastKnownId = lastKnownId?.toString(),
                reverse = true,
            )
        }
    }

    suspend fun sendMessage(channel: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = sessionManager.token ?: throw IllegalStateException("Not authenticated")
            val username = sessionManager.username ?: throw IllegalStateException("Username missing")
            val request = SendMessageRequest.createText(from = username, channel = channel, text = text)
            api.sendMessage(token, request)
            Unit
        }
    }
}
