package com.example.flow.data.local

import androidx.room.*
import com.example.flow.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // OnConflictStrategy.REPLACE means if we insert a task with a duplicate ID,
    // it will be replaced. This is perfect for updates.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    // Using Flow means this function will automatically emit new data
    // whenever the 'tasks' table changes.
    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY completed ASC, priority DESC, deadline ASC")
    fun getAllTasks(userId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: String): Flow<Task?>
}