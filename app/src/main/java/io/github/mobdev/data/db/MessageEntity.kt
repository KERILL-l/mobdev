package io.github.mobdev.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.mobdev.data.ChatMessage
import io.github.mobdev.data.MessageData

@Entity(
    tableName = "messages",
    indices = [Index(value = ["channel"]), Index(value = ["isPending"])],
)
data class MessageEntity(
    @PrimaryKey
    val id: Long,
    val channel: String,
    val fromUser: String,
    val toTarget: String?,
    val text: String?,
    val imageUrl: String?,
    val timestamp: Long?,
    val isPending: Boolean = false,
    val localCreatedAt: Long = System.currentTimeMillis(),
) {
    fun toChatMessage(): ChatMessage {
        val data = when {
            !text.isNullOrBlank() -> MessageData.Text(text)
            !imageUrl.isNullOrBlank() -> MessageData.Image(imageUrl)
            else -> MessageData.Text("")
        }
        return ChatMessage(
            id = id,
            from = fromUser,
            to = toTarget,
            data = data,
            time = timestamp,
            isPending = isPending,
        )
    }

    companion object {
        fun fromChatMessage(message: ChatMessage, channelName: String, pending: Boolean = false): MessageEntity {
            return MessageEntity(
                id = message.id,
                channel = channelName,
                fromUser = message.from,
                toTarget = message.to,
                text = message.textContent,
                imageUrl = message.imageUrl,
                timestamp = message.time,
                isPending = pending,
            )
        }
    }
}
