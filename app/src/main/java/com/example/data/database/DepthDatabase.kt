package com.example.data.database

import androidx.room.*
import com.example.data.model.MessageEntity
import com.example.data.model.SessionEntity
import com.example.data.model.MemoryInsight
import com.example.data.model.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY isPinned DESC, lastUpdatedAt DESC")
    fun getAllSessionsFlow(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions")
    suspend fun getAllSessions(): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSessionIgnore(session: SessionEntity)

    @Query("UPDATE sessions SET lastUpdatedAt = :timestamp WHERE id = :sessionId")
    suspend fun updateLastUsed(sessionId: String, timestamp: Long)

    @Query("UPDATE sessions SET title = :newTitle WHERE id = :sessionId")
    suspend fun renameSession(sessionId: String, newTitle: String)

    @Query("UPDATE sessions SET isPinned = :pinned WHERE id = :sessionId")
    suspend fun setPinned(sessionId: String, pinned: Boolean)

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()

    /**
     * Search sessions where the title matches the query OR any message
     * within that session's history matches the query. Powers the
     * "search recent conversations" feature (title + in-chat content search).
     */
    @Query("""
        SELECT * FROM sessions
        WHERE id IN (
            SELECT DISTINCT s.id FROM sessions s
            LEFT JOIN messages m ON m.sessionId = s.id
            WHERE s.title LIKE '%' || :query || '%'
               OR m.text LIKE '%' || :query || '%'
        )
        ORDER BY isPinned DESC, lastUpdatedAt DESC
    """)
    fun searchSessionsFlow(query: String): Flow<List<SessionEntity>>
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSessionFlow(sessionId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSession(sessionId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessageIgnore(message: MessageEntity)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE text LIKE '%' || :query || '%'")
    suspend fun searchMessages(query: String): List<MessageEntity>
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

@Dao
interface ArchivedInsightDao {
    @Query("SELECT * FROM archived_insights ORDER BY timestamp DESC")
    fun getAllArchivedInsightsFlow(): Flow<List<com.example.data.model.ArchivedInsightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchivedInsight(insight: com.example.data.model.ArchivedInsightEntity)

    @Query("DELETE FROM archived_insights WHERE id = :id")
    suspend fun deleteArchivedInsight(id: String)

    @Query("DELETE FROM archived_insights")
    suspend fun deleteAllArchivedInsights()
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE messageId = :messageId")
    suspend fun getAttachmentsForMessage(messageId: String): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE messageId = :messageId")
    fun getAttachmentsForMessageFlow(messageId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE attachmentId = :attachmentId")
    suspend fun getAttachmentById(attachmentId: String): AttachmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE messageId = :messageId")
    suspend fun deleteAttachmentsForMessage(messageId: String)

    @Query("DELETE FROM attachments WHERE messageId IN (SELECT id FROM messages WHERE sessionId = :sessionId)")
    suspend fun deleteAttachmentsForSession(sessionId: String)
    
    @Query("SELECT * FROM attachments")
    suspend fun getAllAttachments(): List<AttachmentEntity>
}

@Database(entities = [SessionEntity::class, MessageEntity::class, AttachmentEntity::class, MemoryInsight::class, com.example.data.model.ArchivedInsightEntity::class], version = 5, exportSchema = false)
abstract class DepthDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun memoryInsightDao(): MemoryInsightDao
    abstract fun archivedInsightDao(): ArchivedInsightDao

    companion object {
        @Volatile
        private var INSTANCE: DepthDatabase? = null

        fun getDatabase(context: android.content.Context): DepthDatabase {
            return INSTANCE ?: synchronized(this) {
                val dbFile = context.getDatabasePath("depthlens_database")
                val backupFile = java.io.File(context.filesDir, "depthlens_database.bak")
                
                // If dbFile exists, create a backup of it before attempting to open the Room database
                if (dbFile.exists() && dbFile.length() > 0) {
                    try {
                        dbFile.inputStream().use { input ->
                            backupFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        val walFile = java.io.File(dbFile.path + "-wal")
                        if (walFile.exists()) {
                            val walBackup = java.io.File(context.filesDir, "depthlens_database-wal.bak")
                            walFile.inputStream().use { input ->
                                walBackup.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        val shmFile = java.io.File(dbFile.path + "-shm")
                        if (shmFile.exists()) {
                            val shmBackup = java.io.File(context.filesDir, "depthlens_database-shm.bak")
                            shmFile.inputStream().use { input ->
                                shmBackup.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                var instance: DepthDatabase? = null
                try {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        DepthDatabase::class.java,
                        "depthlens_database"
                    )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // Verify integrity of database tables when opened
                            try {
                                db.query("PRAGMA integrity_check").use { cursor ->
                                    if (cursor.moveToFirst()) {
                                        val res = cursor.getString(0)
                                        if (!res.equals("ok", ignoreCase = true)) {
                                            throw android.database.sqlite.SQLiteException("Corruption detected: $res")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                    
                    // Force opening database to verify migration/initialization is successful
                    instance.openHelper.writableDatabase
                } catch (migrationEx: Exception) {
                    migrationEx.printStackTrace()
                    // Reconnection/migration failed! Automatically restore from pre-upgrade backup
                    if (backupFile.exists()) {
                        try {
                            instance?.close()
                        } catch (closeEx: Exception) {}
                        
                        try {
                            backupFile.inputStream().use { input ->
                                dbFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            val walBackup = java.io.File(context.filesDir, "depthlens_database-wal.bak")
                            val walFile = java.io.File(dbFile.path + "-wal")
                            if (walBackup.exists()) {
                                walBackup.inputStream().use { input ->
                                    walFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                            val shmBackup = java.io.File(context.filesDir, "depthlens_database-shm.bak")
                            val shmFile = java.io.File(dbFile.path + "-shm")
                            if (shmBackup.exists()) {
                                shmBackup.inputStream().use { input ->
                                    shmFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                            
                            instance = Room.databaseBuilder(
                                context.applicationContext,
                                DepthDatabase::class.java,
                                "depthlens_database"
                            )
                            .fallbackToDestructiveMigration()
                            .build()
                            instance.openHelper.writableDatabase
                        } catch (restoreEx: Exception) {
                            restoreEx.printStackTrace()
                        }
                    }
                }
                
                val finalInstance = instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DepthDatabase::class.java,
                    "depthlens_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = finalInstance
                finalInstance
            }
        }
    }
}