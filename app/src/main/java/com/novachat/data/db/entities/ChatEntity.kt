package com.novachat.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chats",
    indices = [Index("profileId"), Index("updatedAt")]
)
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String,
    val profileId: String,
    val modelId: String,
    val systemPrompt: String?,
    val temperature: Float,
    val maxTokens: Int?,
    val createdAt: Long,
    val updatedAt: Long
)
