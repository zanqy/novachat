package com.novachat.domain

import com.novachat.domain.model.Chat
import com.novachat.domain.model.ChatMessage
import com.novachat.domain.model.ProviderProfile
import com.novachat.domain.model.Role
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeChats(): Flow<List<Chat>>
    fun observeChat(chatId: String): Flow<Chat?>
    fun observeMessages(chatId: String): Flow<List<ChatMessage>>
    fun observeLastMessage(chatId: String): Flow<ChatMessage?>

    suspend fun createChat(
        profile: ProviderProfile,
        modelId: String?,
        temperature: Float,
        maxTokens: Int?
    ): Chat

    suspend fun renameChat(chatId: String, title: String)
    suspend fun deleteChat(chatId: String)
    suspend fun clearMessages(chatId: String)
    suspend fun updateChatModel(chatId: String, modelId: String)
    suspend fun moveChatToProfile(chatId: String, profileId: String, modelId: String)
    suspend fun updateSystemPrompt(chatId: String, prompt: String?)
    suspend fun touchChat(chatId: String, at: Long)

    suspend fun addMessage(
        chatId: String,
        role: Role,
        content: String,
        modelId: String?
    ): ChatMessage

    suspend fun getMessage(id: String): ChatMessage?
    suspend fun getMessagesOnce(chatId: String): List<ChatMessage>
    suspend fun deleteMessage(id: String)
    suspend fun deleteMessagesAfter(chatId: String, timestamp: Long)
    suspend fun updateMessageContent(id: String, content: String)
    suspend fun finalizeAssistantMessage(
        id: String,
        content: String,
        usageIn: Int?,
        usageOut: Int?,
        errorText: String?,
        stoppedManually: Boolean
    )

    suspend fun counts(): Pair<Int, Int>
    suspend fun deleteAllChats()
}
