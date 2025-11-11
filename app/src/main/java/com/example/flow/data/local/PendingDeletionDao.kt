package com.example.flow.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingDeletionDao {
    /**
     * Inserts a new pending deletion. If one already exists, it's replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pendingDeletion: com.example.flow.data.model.PendingDeletion)

    /**
     * Gets all pending deletion IDs for a specific collection.
     */
    @Query("SELECT id FROM pending_deletions WHERE collectionName = :collectionName")
    suspend fun getPendingDeletionIdsByCollection(collectionName: String): List<String>

    /**
     * Gets all pending deletions.
     */
    @Query("SELECT * FROM pending_deletions")
    suspend fun getAllPendingDeletions(): List<com.example.flow.data.model.PendingDeletion>

    /**
     * Deletes a pending deletion entry. This is called *after*
     * the item has been successfully deleted from Firebase.
     */
    @Delete
    suspend fun delete(pendingDeletion: com.example.flow.data.model.PendingDeletion)
}