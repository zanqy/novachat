package com.novachat.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novachat.data.db.entities.ProviderProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderProfileDao {
    @Query("SELECT * FROM provider_profiles ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<ProviderProfileEntity>>

    @Query("SELECT * FROM provider_profiles WHERE id = :id")
    suspend fun getById(id: String): ProviderProfileEntity?

    @Query("SELECT * FROM provider_profiles WHERE enabled = 1 ORDER BY createdAt ASC")
    suspend fun getEnabled(): List<ProviderProfileEntity>

    @Query("SELECT COUNT(*) FROM provider_profiles")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProviderProfileEntity)

    @Query("DELETE FROM provider_profiles WHERE id = :id")
    suspend fun delete(id: String)
}
