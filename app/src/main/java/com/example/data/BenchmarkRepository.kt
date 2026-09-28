package com.example.data

import kotlinx.coroutines.flow.Flow

class BenchmarkRepository(private val dao: BenchmarkDao) {
    val allSessions: Flow<List<BenchmarkSessionEntity>> = dao.getAllSessions()

    suspend fun saveSession(session: BenchmarkSessionEntity): Long {
        return dao.insertSession(session)
    }

    suspend fun deleteSession(session: BenchmarkSessionEntity) {
        dao.deleteSession(session)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
