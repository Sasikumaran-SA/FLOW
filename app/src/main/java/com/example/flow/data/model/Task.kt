package com.example.flow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// This annotation tells Room to create a table named "tasks"
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val deadline: Long? = null, // Store as a timestamp
    val priority: Int = 1, // 1=Low, 2=Medium, 3=High
    val isCompleted: Boolean = false,
    val listName: String = "Default",
    val userId: String // Crucial for Firebase sync
)