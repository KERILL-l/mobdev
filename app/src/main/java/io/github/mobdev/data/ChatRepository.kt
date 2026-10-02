package io.github.mobdev.data

import io.github.mobdev.data.db.ChannelEntity
import io.github.mobdev.data.db.ChatDao
import io.github.mobdev.data.db.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatRepository(
    private val api: ChatApi = ApiClient.api,
    val sessionManager: SessionManager,
    val chatDao: ChatDao,
    val networkMonitor: NetworkMonitor? = null,
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
            chatDao.clearChannels()
        }
    }

    suspend fun getChannels(): Result<List<String>> = withContext(Dispatchers.IO) {
        val token = sessionManager.token ?: throw IllegalStateException("Необходима авторизация")
        val cached = chatDao.getChannels().map { it.name }
        val isOnline = networkMonitor?.isOnline?.value ?: true

        if (token.startsWith("demo-token")) {
            if (cached.isEmpty()) {
                val entities = DEMO_CHANNELS.mapIndexed { idx, name -> ChannelEntity(name, idx) }
                chatDao.insertChannels(entities)
                return@withContext Result.success(DEMO_CHANNELS)
            }
            return@withContext Result.success(cached)
        }

        if (!isOnline) {
            if (cached.isNotEmpty()) {
                return@withContext Result.success(cached)
            }
            return@withContext Result.failure(IllegalStateException("Офлайн-режим: нет сохраненных каналов"))
        }

        runCatching {
            val remoteChannels = api.getChannels(token)
            val entities = remoteChannels.mapIndexed { idx, name -> ChannelEntity(name, idx) }
            chatDao.insertChannels(entities)
            remoteChannels
        }.recoverCatching { error ->
            if (cached.isNotEmpty()) {
                cached
            } else {
                throw error
            }
        }
    }

    suspend fun getMessages(
        channel: String,
        limit: Int = 20,
        lastKnownId: Long? = null,
    ): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        val token = sessionManager.token ?: throw IllegalStateException("Необходима авторизация")
        val cachedEntities = chatDao.getMessages(channel)
        val isOnline = networkMonitor?.isOnline?.value ?: true

        if (token.startsWith("demo-token")) {
            if (cachedEntities.isEmpty()) {
                val defaults = createDefaultDemoMessages(channel)
                chatDao.insertMessages(defaults.map { MessageEntity.fromChatMessage(it, channel) })
                return@withContext Result.success(defaults)
            }
            return@withContext Result.success(cachedEntities.map { it.toChatMessage() })
        }

        if (!isOnline) {
            return@withContext Result.success(cachedEntities.map { it.toChatMessage() })
        }

        runCatching {
            val remoteMessages = api.getChannelMessages(
                token = token,
                channel = channel,
                limit = limit,
                lastKnownId = lastKnownId?.toString(),
                reverse = true,
            )
            // OnConflictStrategy.REPLACE ensures no duplicate messages in Room!
            val entities = remoteMessages.map { MessageEntity.fromChatMessage(it, channel) }
            chatDao.insertMessages(entities)

            val merged = chatDao.getMessages(channel)
            merged.map { it.toChatMessage() }
        }.recoverCatching { error ->
            if (cachedEntities.isNotEmpty()) {
                cachedEntities.map { it.toChatMessage() }
            } else {
                throw error
            }
        }
    }

    suspend fun sendMessage(channel: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        val token = sessionManager.token ?: throw IllegalStateException("Необходима авторизация")
        val username = sessionManager.username ?: "user"
        val isOnline = networkMonitor?.isOnline?.value ?: true

        // Offline Outbox: save to Room as pending
        if (!isOnline) {
            val pendingEntity = MessageEntity(
                id = -System.currentTimeMillis(),
                channel = channel,
                fromUser = username,
                toTarget = "$channel@channel",
                text = text,
                imageUrl = null,
                timestamp = System.currentTimeMillis(),
                isPending = true,
                localCreatedAt = System.currentTimeMillis(),
            )
            chatDao.insertMessage(pendingEntity)
            return@withContext Result.success(Unit)
        }

        // Flush any pending messages first if we're online
        syncPendingMessages()

        if (token.startsWith("demo-token")) {
            val newMsg = ChatMessage(
                id = System.currentTimeMillis(),
                from = username,
                to = "$channel@channel",
                data = MessageData.Text(text),
                time = System.currentTimeMillis(),
                isPending = false,
            )
            chatDao.insertMessage(MessageEntity.fromChatMessage(newMsg, channel))
            return@withContext Result.success(Unit)
        }

        val request = SendMessageRequest.createText(from = username, channel = channel, text = text)
        runCatching {
            api.sendMessage(token, request)
            Unit
        }.recoverCatching {
            // Save to pending if sending failed (e.g. lost network during call)
            val pendingEntity = MessageEntity(
                id = -System.currentTimeMillis(),
                channel = channel,
                fromUser = username,
                toTarget = "$channel@channel",
                text = text,
                imageUrl = null,
                timestamp = System.currentTimeMillis(),
                isPending = true,
                localCreatedAt = System.currentTimeMillis(),
            )
            chatDao.insertMessage(pendingEntity)
            Unit
        }
    }

    suspend fun syncPendingMessages(): Result<Int> = withContext(Dispatchers.IO) {
        val isOnline = networkMonitor?.isOnline?.value ?: true
        if (!isOnline) return@withContext Result.success(0)

        val token = sessionManager.token ?: return@withContext Result.success(0)
        val pendingList = chatDao.getPendingMessages()
        if (pendingList.isEmpty()) return@withContext Result.success(0)

        var syncedCount = 0
        for (msg in pendingList) {
            val text = msg.text ?: continue
            if (token.startsWith("demo-token")) {
                chatDao.markMessageSent(msg.id, System.currentTimeMillis(), System.currentTimeMillis())
                syncedCount++
                continue
            }

            try {
                val request = SendMessageRequest.createText(from = msg.fromUser, channel = msg.channel, text = text)
                api.sendMessage(token, request)
                chatDao.deleteMessage(msg.id)
                syncedCount++
            } catch (e: Exception) {
                // Network failed mid-sync, leave remaining in queue
                break
            }
        }
        Result.success(syncedCount)
    }

    companion object {
        private val DEMO_CHANNELS = listOf("general", "random", "flood", "dev")

        private fun createDefaultDemoMessages(channel: String): MutableList<ChatMessage> {
            val now = System.currentTimeMillis()
            return mutableListOf(
                ChatMessage(
                    id = 1,
                    from = "Преподаватель",
                    to = "$channel@channel",
                    data = MessageData.Text("Добро пожаловать в канал #$channel! Лабораторная работа №4 (кэширование в Room) успешно подключена."),
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
                    data = MessageData.Text("База данных Room активна. Попробуйте отключить интернет и перезапустить приложение — все сообщения сохранятся!"),
                    time = now - 600_000,
                ),
            )
        }
    }
}
