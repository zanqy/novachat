package com.novachat.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.novachat.data.db.entities.ChatEntity
import com.novachat.data.db.entities.MessageEntity
import com.novachat.data.db.entities.ProviderProfileEntity

@Database(
    entities = [ChatEntity::class, MessageEntity::class, ProviderProfileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NovaChatDb : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun providerProfileDao(): ProviderProfileDao
}
