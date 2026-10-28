package com.novachat.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [Index("chatId"), Index("createdAt")]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    /** "USER" | "ASSISTANT" */
    val role: String,
    val content: String,
    val errorText: String?,
    val modelId: String?,
    val usageIn: Int?,
    val usageOut: Int?,
    val stoppedManually: Boolean,
    val createdAt: Long
)
