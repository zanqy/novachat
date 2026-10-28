package com.novachat.domain

import com.novachat.domain.model.ChatEvent
import com.novachat.domain.model.ChatMessage
import com.novachat.domain.model.ModelInfo
import com.novachat.domain.model.ProviderProfile

interface ChatProvider {
    suspend fun stream(
        profile: ProviderProfile,
        modelId: String,
        messages: List<ChatMessage>,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int?,
        onEvent: (ChatEvent) -> Unit
    )

    suspend fun listModels(profile: ProviderProfile): List<ModelInfo>
}
