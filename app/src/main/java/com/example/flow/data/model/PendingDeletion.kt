package com.example.flow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_deletions")
data class PendingDeletion(
    @PrimaryKey
    val id: String,
    val collectionName: String
)