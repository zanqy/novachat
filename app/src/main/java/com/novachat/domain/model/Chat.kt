package com.novachat.domain.model

data class Chat(
    val id: String,
    val title: String,
    val profileId: String,
    val modelId: String,
    val systemPrompt: String?,
    val temperature: Float,
    val maxTokens: Int?,
    val createdAt: Long,
    val updatedAt: Long
)
