package com.novachat.domain.model

enum class ProviderKind { OPENAI_COMPATIBLE, ANTHROPIC, GEMINI, ECHO }

data class ProviderProfile(
    val id: String,
    val kind: ProviderKind,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val defaultModelId: String,
    val enabled: Boolean,
    val createdAt: Long
)
