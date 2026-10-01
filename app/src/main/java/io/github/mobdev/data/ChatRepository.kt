package io.github.mobdev.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatRepository(
    private val api: ChatApi = ApiClient.api,
    val sessionManager: SessionManager,
) {

    suspend fun login(name: String, pwd: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        val trimmedPwd = pwd.trim()

        // 1. Demo Mode: allows instant testing without server account
        if (trimmedName.equals("demo", ignoreCase = true) || trimmedPwd.equals("demo", ignoreCase = true)) {
            val demoToken = "demo-token-$trimmedName"
            sessionManager.saveSession(username = trimmedName, token = demoToken)
            return@withContext Result.success(demoToken)
        }

        // 2. Real faerytea.name server authentication
        runCatching {
            val responseBody = api.login(LoginRequest(trimmedName, trimmedPwd))
            val rawToken = responseBody.string().trim()
            val token = rawToken.removeSurrounding("\"").trim()
            if (token.isBlank() || token.contains("Wrong login", ignoreCase = true)) {
                throw IllegalStateException("Неверный логин или пароль на сервере (или используйте demo / demo для теста)")
            }
            sessionManager.saveSession(username = trimmedName, token = token)
            token
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = sessionManager.token
            if (!token.isNullOrBlank() && !token.startsWith("demo-token")) {
                try {
                    api.logout(token)
                } catch (e: Exception) {
                    // Ignore network logout errors
                }
            }
            sessionManager.clear()
        }
    }

    suspend fun getChannels(): Result<List<String>> = withContext(Dispatchers.IO) {
        val token = sessionManager.token ?: throw IllegalStateException("Необходима авторизация")

        if (token.startsWith("demo-token")) {
            return@withContext Result.success(DEMO_CHANNELS)
        }

        runCatching {
            api.getChannels(token)
        }
    }

    suspend fun getMessages(
        channel: String,
        limit: Int = 30,
        lastKnownId: Long? = null,
    ): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        val token = sessionManager.token ?: throw IllegalStateException("Необходима авторизация")

        if (token.startsWith("demo-token")) {
            val list = demoChannelMessages.getOrPut(channel) {
                createDefaultDemoMessages(channel)
            }
            return@withContext Result.success(list.toList())
        }

        runCatching {
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
        val token = sessionManager.token ?: throw IllegalStateException("Необходима авторизация")
        val username = sessionManager.username ?: "user"

        if (token.startsWith("demo-token")) {
            val list = demoChannelMessages.getOrPut(channel) {
                createDefaultDemoMessages(channel)
            }
            val newMsg = ChatMessage(
                id = System.currentTimeMillis(),
                from = username,
                to = "$channel@channel",
                data = MessageData.Text(text),
                time = System.currentTimeMillis(),
            )
            list.add(newMsg)
            return@withContext Result.success(Unit)
        }

        runCatching {
            val request = SendMessageRequest.createText(from = username, channel = channel, text = text)
            api.sendMessage(token, request)
            Unit
        }
    }

    companion object {
        private val DEMO_CHANNELS = listOf("general", "random", "flood", "dev")

        private val demoChannelMessages = mutableMapOf<String, MutableList<ChatMessage>>()

        private fun createDefaultDemoMessages(channel: String): MutableList<ChatMessage> {
            val now = System.currentTimeMillis()
            return mutableListOf(
                ChatMessage(
                    id = 1,
                    from = "Преподаватель",
                    to = "$channel@channel",
                    data = MessageData.Text("Добро пожаловать в канал #$channel! Лабораторная работа №3 успешно подключена."),
                    time = now - 3600_000,
                ),
                ChatMessage(
                    id = 2,
                    from = "Студент_Иван",
                    to = "$channel@channel",
                    data = MessageData.Text("Привет! Отправляю скриншот работы:"),
                    time = now - 1800_000,
                ),
                ChatMessage(
                    id = 3,
                    from = "Студент_Иван",
                    to = "$channel@channel",
                    data = MessageData.Image("https://picsum.photos/400/250"),
                    time = now - 1700_000,
                ),
                ChatMessage(
                    id = 4,
                    from = "Бот",
                    to = "$channel@channel",
                    data = MessageData.Text("Тестовое окружение готово. Напишите сообщение ниже, чтобы проверить отправку!"),
                    time = now - 600_000,
                ),
            )
        }
    }
}
