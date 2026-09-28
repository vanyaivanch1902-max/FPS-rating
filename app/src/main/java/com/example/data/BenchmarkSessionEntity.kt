package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "benchmark_sessions")
data class BenchmarkSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long,
    val avgFps: Float,
    val minFps: Int,
    val maxFps: Int,
    val onePercentLow: Float,
    val stutterCount: Int,
    val targetFps: Int,
    val batteryTempStart: Float,
    val batteryTempEnd: Float,
    val notes: String = ""
)
