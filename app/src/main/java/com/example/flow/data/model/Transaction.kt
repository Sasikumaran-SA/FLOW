package com.example.flow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val date: Long = 0L,
    val isIncome: Boolean = false,
    val category: String = ""
)