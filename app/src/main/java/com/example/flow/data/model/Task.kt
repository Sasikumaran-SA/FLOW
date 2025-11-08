package com.example.flow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// This annotation tells Room to create a table named "tasks"
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(), // ADDED default
    val userId: String = "",                       // ADDED default
    val title: String = "",                        // ADDED default
    val description: String? = null,
    val deadline: Long? = null,
    val priority: Int = 1,                         // ADDED default
    val listName: String = "Default",              // ADDED default
    val isCompleted: Boolean = false               // ADDED default
)