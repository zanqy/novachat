package com.novachat.data.chat

import com.novachat.data.db.ChatDao
import com.novachat.data.db.MessageDao
import com.novachat.data.db.entities.ChatEntity
import com.novachat.data.db.entities.MessageEntity
import com.novachat.domain.ChatRepository
import com.novachat.domain.model.Chat
import com.novachat.domain.model.ChatMessage
import com.novachat.domain.model.ProviderProfile
import com.novachat.domain.model.Role
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomChatRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) : ChatRepository {

    override fun observeChats(): Flow<List<Chat>> =
        chatDao.observeChats().map { list -> list.map { it.toDomain() } }

    override fun observeChat(chatId: String): Flow<Chat?> =
        chatDao.observeChat(chatId).map { it?.toDomain() }

    override fun observeMessages(chatId: String): Flow<List<ChatMessage>> =
        messageDao.observeMessages(chatId).map { list -> list.map { it.toDomain() } }

    override fun observeLastMessage(chatId: String): Flow<ChatMessage?> =
        messageDao.observeLastMessage(chatId).map { it?.toDomain() }

    override suspend fun createChat(
        profile: ProviderProfile,
        modelId: String?,
        temperature: Float,
        maxTokens: Int?
    ): Chat {
        val now = System.currentTimeMillis()
        val entity = ChatEntity(
            id = UUID.randomUUID().toString(),
            title = "New chat",
            profileId = profile.id,
            modelId = modelId ?: profile.defaultModelId,
            systemPrompt = null,
            temperature = temperature,
            maxTokens = maxTokens,
            createdAt = now,
            updatedAt = now
        )
        chatDao.upsert(entity)
        return entity.toDomain()
    }

    override suspend fun renameChat(chatId: String, title: String) {
        chatDao.rename(chatId, title.ifBlank { "Untitled" })
    }

    override suspend fun deleteChat(chatId: String) {
        messageDao.deleteByChat(chatId)
        chatDao.delete(chatId)
    }

    override suspend fun clearMessages(chatId: String) {
        messageDao.deleteByChat(chatId)
        chatDao.touch(chatId, System.currentTimeMillis())
    }

    override suspend fun updateChatModel(chatId: String, modelId: String) {
        chatDao.updateModel(chatId, modelId)
    }

    override suspend fun moveChatToProfile(chatId: String, profileId: String, modelId: String) {
        chatDao.moveToProfile(chatId, profileId, modelId)
    }

    override suspend fun updateSystemPrompt(chatId: String, prompt: String?) {
        chatDao.updateSystemPrompt(chatId, prompt?.ifBlank { null })
    }

    override suspend fun touchChat(chatId: String, at: Long) {
        chatDao.touch(chatId, at)
    }

    override suspend fun addMessage(
        chatId: String,
        role: Role,
        content: String,
        modelId: String?
    ): ChatMessage {
        val entity = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            role = role.name,
            content = content,
            errorText = null,
            modelId = modelId,
            usageIn = null,
            usageOut = null,
            stoppedManually = false,
            createdAt = System.currentTimeMillis()
        )
        messageDao.insert(entity)
        chatDao.touch(chatId, entity.createdAt)
        return entity.toDomain()
    }

    override suspend fun getMessage(id: String): ChatMessage? =
        messageDao.getById(id)?.toDomain()

    override suspend fun getMessagesOnce(chatId: String): List<ChatMessage> =
        messageDao.getOnce(chatId).map { it.toDomain() }

    override suspend fun deleteMessage(id: String) {
        messageDao.deleteById(id)
    }

    override suspend fun deleteMessagesAfter(chatId: String, timestamp: Long) {
        messageDao.deleteAfter(chatId, timestamp)
    }

    override suspend fun updateMessageContent(id: String, content: String) {
        messageDao.updateContent(id, content)
    }

    override suspend fun finalizeAssistantMessage(
        id: String,
        content: String,
        usageIn: Int?,
        usageOut: Int?,
        errorText: String?,
        stoppedManually: Boolean
    ) {
        messageDao.finalizeMessage(id, content, usageIn, usageOut, errorText, stoppedManually)
    }

    override suspend fun counts(): Pair<Int, Int> =
        chatDao.count() to messageDao.count()

    override suspend fun deleteAllChats() {
        messageDao.deleteAll()
        chatDao.deleteAll()
    }

    private fun ChatEntity.toDomain() = Chat(
        id = id,
        title = title,
        profileId = profileId,
        modelId = modelId,
        systemPrompt = systemPrompt,
        temperature = temperature,
        maxTokens = maxTokens,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun MessageEntity.toDomain() = ChatMessage(
        id = id,
        chatId = chatId,
        role = runCatching { Role.valueOf(role) }.getOrDefault(Role.USER),
        content = content,
        errorText = errorText,
        modelId = modelId,
        usageIn = usageIn,
        usageOut = usageOut,
        stoppedManually = stoppedManually,
        createdAt = createdAt
    )
}
