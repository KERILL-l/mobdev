package io.github.mobdev.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    // --- Channels ---
    @Query("SELECT * FROM channels ORDER BY orderIndex ASC")
    fun getChannelsFlow(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels ORDER BY orderIndex ASC")
    suspend fun getChannels(): List<ChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels")
    suspend fun clearChannels()

    // --- Messages ---
    @Query("SELECT * FROM messages WHERE channel = :channel ORDER BY CASE WHEN timestamp IS NULL OR timestamp = 0 THEN localCreatedAt ELSE timestamp END ASC")
    fun getMessagesFlow(channel: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE channel = :channel ORDER BY CASE WHEN timestamp IS NULL OR timestamp = 0 THEN localCreatedAt ELSE timestamp END ASC")
    suspend fun getMessages(channel: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("SELECT * FROM messages WHERE isPending = 1 ORDER BY localCreatedAt ASC")
    suspend fun getPendingMessages(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: Long): MessageEntity?

    @Transaction
    suspend fun markMessageSent(localId: Long, serverId: Long, sentTime: Long) {
        if (localId != serverId) {
            val pending = getMessageById(localId)
            if (pending != null) {
                deleteMessage(localId)
                insertMessage(
                    pending.copy(
                        id = serverId,
                        timestamp = sentTime,
                        isPending = false,
                    ),
                )
            }
        } else {
            val pending = getMessageById(localId)
            if (pending != null) {
                insertMessage(
                    pending.copy(
                        timestamp = sentTime,
                        isPending = false,
                    ),
                )
            }
        }
    }
}
