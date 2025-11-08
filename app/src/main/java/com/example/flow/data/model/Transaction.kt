package com.example.flow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val description: String,
    val amount: Double,
    val date: Long, // Timestamp
    val isIncome: Boolean, // false = expense
    val category: String,
    val receiptImageUrl: String? = null, // URL from Firebase Storage
    val userId: String // For Firebase sync
)