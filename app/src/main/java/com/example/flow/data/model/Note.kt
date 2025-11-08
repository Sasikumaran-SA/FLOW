package com.example.flow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val isLocked: Boolean = false,
    // We store a simple hash of the password.
    // In a real app, use androidx.security.crypto to encrypt the content itself.
    val passwordHash: String? = null,
    val lastModified: Long = System.currentTimeMillis(),
    val userId: String
)