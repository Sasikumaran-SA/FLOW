package com.example.flow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val locked: Boolean = false,
    val passwordHash: String? = null,
    val lastModified: Long = 0L
)