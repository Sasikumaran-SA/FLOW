package com.example.flow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(), // ADDED default
    val userId: String = "",                       // ADDED default
    val description: String = "",                  // ADDED default
    val amount: Double = 0.0,                      // ADDED default
    val date: Long = 0L,                           // ADDED default
    val isIncome: Boolean = false,                 // ADDED default
    val category: String = "",                     // ADDED default
    val receiptImageUrl: String? = null            // Already nullable, which is fine
)