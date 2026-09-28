package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BenchmarkDao {
    @Query("SELECT * FROM benchmark_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<BenchmarkSessionEntity>>

    @Query("SELECT * FROM benchmark_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): BenchmarkSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: BenchmarkSessionEntity): Long

    @Delete
    suspend fun deleteSession(session: BenchmarkSessionEntity)

    @Query("DELETE FROM benchmark_sessions")
    suspend fun clearAll()
}
