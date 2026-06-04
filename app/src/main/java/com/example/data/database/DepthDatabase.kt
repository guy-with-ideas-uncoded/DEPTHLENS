package com.example.data.database

import androidx.room.*
import com.example.data.model.MessageEntity
import com.example.data.model.SessionEntity
import com.example.data.model.MemoryInsight
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY isPinned DESC, lastUpdatedAt DESC")
    fun getAllSessionsFlow(): Flow<List<SessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Query("UPDATE sessions SET lastUpdatedAt = :timestamp WHERE id = :sessionId")
    suspend fun updateLastUsed(sessionId: String, timestamp: Long)

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSessionFlow(sessionId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSession(sessionId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)
}

@Dao
interface MemoryInsightDao {
    @Query("SELECT * FROM memory_insights ORDER BY timestamp DESC")
    fun getAllInsightsFlow(): Flow<List<MemoryInsight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: MemoryInsight)

    @Query("DELETE FROM memory_insights WHERE id = :id")
    suspend fun deleteInsight(id: Long)

    @Query("DELETE FROM memory_insights")
    suspend fun deleteAllInsights()
}

@Database(entities = [SessionEntity::class, MessageEntity::class, MemoryInsight::class], version = 2, exportSchema = false)
abstract class DepthDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryInsightDao(): MemoryInsightDao

    companion object {
        @Volatile
        private var INSTANCE: DepthDatabase? = null

        fun getDatabase(context: android.content.Context): DepthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DepthDatabase::class.java,
                    "depthlens_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
