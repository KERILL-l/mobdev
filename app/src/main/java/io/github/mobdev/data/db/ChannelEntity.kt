package io.github.mobdev.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey
    val name: String,
    val orderIndex: Int = 0,
)
