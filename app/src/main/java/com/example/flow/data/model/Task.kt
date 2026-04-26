package com.example.flow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String = "",
    val description: String? = null,
    val deadline: Long? = null,
    val priority: Int = 1,
    val listName: String = "Default",
    val completed: Boolean = false
)