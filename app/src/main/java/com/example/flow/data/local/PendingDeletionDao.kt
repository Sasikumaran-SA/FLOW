package com.example.flow.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingDeletionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pendingDeletion: com.example.flow.data.model.PendingDeletion)

    @Query("SELECT id FROM pending_deletions WHERE collectionName = :collectionName")
    suspend fun getPendingDeletionIdsByCollection(collectionName: String): List<String>

    @Query("SELECT * FROM pending_deletions")
    suspend fun getAllPendingDeletions(): List<com.example.flow.data.model.PendingDeletion>

    @Delete
    suspend fun delete(pendingDeletion: com.example.flow.data.model.PendingDeletion)
}