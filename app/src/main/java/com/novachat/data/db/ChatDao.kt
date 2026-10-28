package com.novachat.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novachat.data.db.entities.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    fun observeChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :id")
    fun observeChat(id: String): Flow<ChatEntity?>

    @Query("SELECT * FROM chats WHERE id = :id")
    suspend fun getChat(id: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(chat: ChatEntity)

    @Query("UPDATE chats SET title = :title WHERE id = :id")
    suspend fun rename(id: String, title: String)

    @Query("UPDATE chats SET modelId = :modelId WHERE id = :id")
    suspend fun updateModel(id: String, modelId: String)

    @Query("UPDATE chats SET profileId = :profileId, modelId = :modelId WHERE id = :id")
    suspend fun moveToProfile(id: String, profileId: String, modelId: String)

    @Query("UPDATE chats SET systemPrompt = :prompt WHERE id = :id")
    suspend fun updateSystemPrompt(id: String, prompt: String?)

    @Query("UPDATE chats SET updatedAt = :at WHERE id = :id")
    suspend fun touch(id: String, at: Long)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM chats")
    suspend fun count(): Int

    @Query("DELETE FROM chats")
    suspend fun deleteAll()
}
