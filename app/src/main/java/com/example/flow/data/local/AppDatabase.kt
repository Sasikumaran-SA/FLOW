package com.example.flow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.flow.data.model.Note
import com.example.flow.data.model.PendingDeletion // --- ADD THIS IMPORT ---
import com.example.flow.data.model.Task
import com.example.flow.data.model.Transaction

@Database(
    entities = [
        Task::class,
        Transaction::class,
        Note::class,
        PendingDeletion::class // --- ADD THIS LINE ---
    ],
    version = 2, // --- INCREMENT YOUR DATABASE VERSION ---
    exportSchema = false
)
public abstract class AppDatabase : RoomDatabase() {

    public abstract fun taskDao(): TaskDao
    public abstract fun transactionDao(): TransactionDao
    public abstract fun noteDao(): NoteDao
    public abstract fun pendingDeletionDao(): PendingDeletionDao // --- ADD THIS LINE ---

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flow_database"
                )
                    // --- ADD MIGRATION IF version > 1 ---
                    // You must add a migration or use fallbackToDestructiveMigration
                    .fallbackToDestructiveMigration() // Easiest for development
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}