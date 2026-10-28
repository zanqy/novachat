package com.novachat.data.providers

import com.novachat.data.db.ProviderProfileDao
import com.novachat.data.db.entities.ProviderProfileEntity
import com.novachat.domain.ProviderRepository
import com.novachat.domain.model.ProviderKind
import com.novachat.domain.model.ProviderProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomProviderRepository(
    private val dao: ProviderProfileDao
) : ProviderRepository {

    override fun observeProfiles(): Flow<List<ProviderProfile>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getProfile(id: String): ProviderProfile? =
        dao.getById(id)?.toDomain()

    override suspend fun defaultProfile(): ProviderProfile? =
        dao.getEnabled().firstOrNull()?.toDomain()

    override suspend fun upsert(profile: ProviderProfile) {
        dao.upsert(profile.toEntity())
    }

    override suspend fun deleteProfile(id: String) {
        dao.delete(id)
    }

    override suspend fun count(): Int = dao.count()

    private fun ProviderProfileEntity.toDomain() = ProviderProfile(
        id = id,
        kind = runCatching { ProviderKind.valueOf(kind) }
            .getOrDefault(ProviderKind.OPENAI_COMPATIBLE),
        name = name,
        baseUrl = baseUrl,
        apiKey = apiKey,
        defaultModelId = defaultModelId,
        enabled = enabled,
        createdAt = createdAt
    )

    private fun ProviderProfile.toEntity() = ProviderProfileEntity(
        id = id,
        kind = kind.name,
        name = name,
        baseUrl = baseUrl,
        apiKey = apiKey,
        defaultModelId = defaultModelId,
        enabled = enabled,
        createdAt = createdAt
    )
}
