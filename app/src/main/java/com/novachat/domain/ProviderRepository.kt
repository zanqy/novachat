package com.novachat.domain

import com.novachat.domain.model.ProviderProfile
import kotlinx.coroutines.flow.Flow

interface ProviderRepository {
    fun observeProfiles(): Flow<List<ProviderProfile>>
    suspend fun getProfile(id: String): ProviderProfile?
    suspend fun defaultProfile(): ProviderProfile?
    suspend fun upsert(profile: ProviderProfile)
    suspend fun deleteProfile(id: String)
    suspend fun count(): Int
}
