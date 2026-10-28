package com.novachat.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "provider_profiles")
data class ProviderProfileEntity(
    @PrimaryKey val id: String,
    /** "OPENAI_COMPATIBLE" | "ANTHROPIC" | "GEMINI" | "ECHO" */
    val kind: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val defaultModelId: String,
    val enabled: Boolean,
    val createdAt: Long
)
