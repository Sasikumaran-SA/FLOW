package com.example.flow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(), // ADDED default
    val userId: String = "",                       // ADDED default
    val title: String = "",                        // ADDED default
    val content: String = "",                      // ADDED default
    val isLocked: Boolean = false,                 // ADDED default
    val passwordHash: String? = null,
    val lastModified: Long = 0L                    // ADDED default
)