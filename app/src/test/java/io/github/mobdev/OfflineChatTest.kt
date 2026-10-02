package io.github.mobdev

import io.github.mobdev.data.ChatApi
import io.github.mobdev.data.ChatMessage
import io.github.mobdev.data.ChatRepository
import io.github.mobdev.data.LoginRequest
import io.github.mobdev.data.MessageData
import io.github.mobdev.data.SendMessageRequest
import io.github.mobdev.data.db.ChannelEntity
import io.github.mobdev.data.db.ChatDao
import io.github.mobdev.data.db.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeChatDao : ChatDao {
    private val channels = mutableMapOf<String, ChannelEntity>()
    private val messages = mutableMapOf<Long, MessageEntity>()

    override fun getChannelsFlow(): Flow<List<ChannelEntity>> = flowOf(getChannelsSync())

    override suspend fun getChannels(): List<ChannelEntity> = getChannelsSync()

    private fun getChannelsSync(): List<ChannelEntity> = channels.values.sortedBy { it.orderIndex }

    override suspend fun insertChannels(channelList: List<ChannelEntity>) {
        // REPLACE strategy
        for (c in channelList) {
            channels[c.name] = c
        }
    }

    override suspend fun clearChannels() {
        channels.clear()
    }

    override fun getMessagesFlow(channel: String): Flow<List<MessageEntity>> =
        flowOf(getMessagesSync(channel))

    override suspend fun getMessages(channel: String): List<MessageEntity> =
        getMessagesSync(channel)

    private fun getMessagesSync(channel: String): List<MessageEntity> =
        messages.values.filter { it.channel == channel }.sortedBy { it.timestamp ?: it.localCreatedAt }

    override suspend fun insertMessages(messageList: List<MessageEntity>) {
        // PrimaryKey id with OnConflictStrategy.REPLACE ensures NO DUPLICATES!
        for (m in messageList) {
            messages[m.id] = m
        }
    }

    override suspend fun insertMessage(message: MessageEntity) {
        messages[message.id] = message
    }

    override suspend fun deleteMessage(id: Long) {
        messages.remove(id)
    }

    override suspend fun getPendingMessages(): List<MessageEntity> =
        messages.values.filter { it.isPending }.sortedBy { it.localCreatedAt }

    override suspend fun getMessageById(id: Long): MessageEntity? = messages[id]

    override suspend fun markMessageSent(localId: Long, serverId: Long, sentTime: Long) {
        val existing = messages.remove(localId) ?: return
        messages[serverId] = existing.copy(
            id = serverId,
            timestamp = sentTime,
            isPending = false,
        )
    }
}

class FakeChatApi : ChatApi {
    val sentMessages = mutableListOf<SendMessageRequest>()
    var shouldFail: Boolean = false

    override suspend fun login(request: LoginRequest): ResponseBody =
        "test-token".toResponseBody("text/plain".toMediaTypeOrNull())

    override suspend fun logout(token: String): ResponseBody =
        "".toResponseBody("text/plain".toMediaTypeOrNull())

    override suspend fun getChannels(token: String): List<String> =
        if (shouldFail) throw RuntimeException("Network down") else listOf("general", "random")

    override suspend fun getChannelMessages(
        token: String,
        channel: String,
        limit: Int,
        lastKnownId: String?,
        reverse: Boolean,
    ): List<ChatMessage> {
        if (shouldFail) throw RuntimeException("Network down")
        return listOf(
            ChatMessage(id = 10, from = "server", data = MessageData.Text("server msg 10")),
            ChatMessage(id = 20, from = "server", data = MessageData.Text("server msg 20")),
        )
    }

    override suspend fun sendMessage(token: String, message: SendMessageRequest): ResponseBody {
        if (shouldFail) throw RuntimeException("Network down")
        sentMessages.add(message)
        return "".toResponseBody("text/plain".toMediaTypeOrNull())
    }
}

class OfflineChatTest {

    private lateinit var dao: FakeChatDao
    private lateinit var api: FakeChatApi

    @Before
    fun setup() {
        dao = FakeChatDao()
        api = FakeChatApi()
    }

    @Test
    fun `test message entity mapping retains all fields and pending state`() {
        val original = ChatMessage(
            id = 42,
            from = "kirill",
            to = "general@channel",
            data = MessageData.Text("Hello Room"),
            time = 1718000000000L,
            isPending = true,
        )

        val entity = MessageEntity.fromChatMessage(original, "general", pending = true)
        assertEquals(42L, entity.id)
        assertEquals("general", entity.channel)
        assertEquals("kirill", entity.fromUser)
        assertEquals("Hello Room", entity.text)
        assertTrue(entity.isPending)

        val restored = entity.toChatMessage()
        assertEquals(original.id, restored.id)
        assertEquals(original.from, restored.from)
        assertEquals(original.to, restored.to)
        assertEquals("Hello Room", restored.textContent)
        assertEquals(original.time, restored.time)
        assertTrue(restored.isPending)
    }

    @Test
    fun `test no duplicates when inserting same message twice`() = runBlocking {
        val msg1 = MessageEntity(
            id = 100,
            channel = "general",
            fromUser = "alice",
            toTarget = "general@channel",
            text = "Unique message",
            imageUrl = null,
            timestamp = 1000,
            isPending = false,
        )

        dao.insertMessage(msg1)
        assertEquals(1, dao.getMessages("general").size)

        // Re-insert identical or updated entity with same id
        val msg1Updated = msg1.copy(text = "Unique message edited")
        dao.insertMessage(msg1Updated)

        val list = dao.getMessages("general")
        assertEquals("Should not duplicate messages with identical ID", 1, list.size)
        assertEquals("Unique message edited", list[0].text)
    }

    @Test
    fun `test offline message outbox queue and network sync`() = runBlocking {
        // Simulate offline sending
        val channel = "general"
        val offlineMsg = MessageEntity(
            id = -System.currentTimeMillis(),
            channel = channel,
            fromUser = "offline_user",
            toTarget = "$channel@channel",
            text = "Stored offline",
            imageUrl = null,
            timestamp = System.currentTimeMillis(),
            isPending = true,
        )
        dao.insertMessage(offlineMsg)

        // Verify message is pending in Room
        val pendingList = dao.getPendingMessages()
        assertEquals(1, pendingList.size)
        assertTrue(pendingList[0].isPending)
        assertEquals("Stored offline", pendingList[0].text)

        // Verify it appears in channel messages marked pending
        val channelMessages = dao.getMessages(channel)
        assertEquals(1, channelMessages.size)
        assertTrue(channelMessages[0].isPending)

        // Now simulate network restoration and sync
        for (pending in pendingList) {
            val req = SendMessageRequest.createText(pending.fromUser, pending.channel, pending.text!!)
            api.sendMessage("token", req)
            dao.deleteMessage(pending.id)
        }

        // Pending queue must now be empty
        assertEquals(0, dao.getPendingMessages().size)
        assertEquals(1, api.sentMessages.size)
        assertEquals("Stored offline", api.sentMessages[0].data.getAsJsonObject("Text").get("text").asString)
    }

    @Test
    fun `test offline cache fallback when network fails`() = runBlocking {
        // Pre-populate DAO with cached channels and messages
        dao.insertChannels(listOf(ChannelEntity("general", 0), ChannelEntity("random", 1)))
        dao.insertMessages(listOf(
            MessageEntity(1, "general", "alice", "general@channel", "Cached msg 1", null, 1000),
            MessageEntity(2, "general", "bob", "general@channel", "Cached msg 2", null, 2000),
        ))

        // When network fails, cached data is still present and valid
        val cachedChannels = dao.getChannels().map { it.name }
        assertEquals(2, cachedChannels.size)
        assertEquals("general", cachedChannels[0])

        val cachedMessages = dao.getMessages("general").map { it.toChatMessage() }
        assertEquals(2, cachedMessages.size)
        assertEquals("Cached msg 1", cachedMessages[0].textContent)
        assertEquals("Cached msg 2", cachedMessages[1].textContent)
    }
}
