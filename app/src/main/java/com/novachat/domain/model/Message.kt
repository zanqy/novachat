package com.novachat.domain.model

enum class Role { USER, ASSISTANT }

data class ChatMessage(
    val id: String,
    val chatId: String,
    val role: Role,
    val content: String,
    val errorText: String?,
    val modelId: String?,
    val usageIn: Int?,
    val usageOut: Int?,
    val stoppedManually: Boolean,
    val createdAt: Long
)
