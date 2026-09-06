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

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): SessionEntity?

    @Query("SELECT * FROM sessions")
    suspend fun getAllSessions(): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SessionEntity>)

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

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

    @Query("SELECT * FROM attachments WHERE localUri = :localUri LIMIT 1")
    suspend fun getAttachmentByLocalUri(localUri: String): AttachmentEntity?

    @Query("SELECT * FROM attachments WHERE remoteUrl = :remoteUrl LIMIT 1")
    suspend fun getAttachmentByRemoteUrl(remoteUrl: String): AttachmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Query("DELETE FROM attachments WHERE messageId = :messageId")
    suspend fun deleteAttachmentsForMessage(messageId: String)

    @Query("DELETE FROM attachments WHERE messageId IN (SELECT id FROM messages WHERE sessionId = :sessionId)")
    suspend fun deleteAttachmentsForSession(sessionId: String)
    
    @Query("SELECT * FROM attachments")
    suspend fun getAllAttachments(): List<AttachmentEntity>
}

@Database(entities = [SessionEntity::class, MessageEntity::class, AttachmentEntity::class, MemoryInsight::class, com.example.data.model.ArchivedInsightEntity::class], version = 6, exportSchema = false)
abstract class DepthDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun memoryInsightDao(): MemoryInsightDao
    abstract fun archivedInsightDao(): ArchivedInsightDao

    companion object {
        private fun migrateAnyTo6(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            // 1. Ensure sessions table exists with all required columns
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `sessions` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `title` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `lastUpdatedAt` INTEGER NOT NULL,
                    `isPinned` INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
            
            try {
                val sessionCols = mutableSetOf<String>()
                db.query("PRAGMA table_info(`sessions`)").use { cursor ->
                    val nameIdx = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        if (nameIdx >= 0) sessionCols.add(cursor.getString(nameIdx))
                    }
                }
                if (!sessionCols.contains("isPinned")) {
                    db.execSQL("ALTER TABLE `sessions` ADD COLUMN `isPinned` INTEGER NOT NULL DEFAULT 0")
                }
            } catch (e: Exception) {
                android.util.Log.e("DB_MIGRATION", "Error checking columns for sessions", e)
            }

            // 2. Ensure messages table exists
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `messages` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `sessionId` TEXT NOT NULL,
                    `role` TEXT NOT NULL,
                    `text` TEXT NOT NULL,
                    `imageUri` TEXT,
                    `timestamp` INTEGER NOT NULL,
                    `replyToMessageId` TEXT,
                    `selectedText` TEXT
                )
            """.trimIndent())
            
            try {
                val msgCols = mutableSetOf<String>()
                db.query("PRAGMA table_info(`messages`)").use { cursor ->
                    val nameIdx = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        if (nameIdx >= 0) msgCols.add(cursor.getString(nameIdx))
                    }
                }
                if (!msgCols.contains("replyToMessageId")) {
                    db.execSQL("ALTER TABLE `messages` ADD COLUMN `replyToMessageId` TEXT")
                }
                if (!msgCols.contains("selectedText")) {
                    db.execSQL("ALTER TABLE `messages` ADD COLUMN `selectedText` TEXT")
                }
                if (!msgCols.contains("imageUri")) {
                    db.execSQL("ALTER TABLE `messages` ADD COLUMN `imageUri` TEXT")
                }
            } catch (e: Exception) {
                android.util.Log.e("DB_MIGRATION", "Error checking columns for messages", e)
            }

            // 3. Ensure attachments table exists
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `attachments` (
                    `attachmentId` TEXT NOT NULL PRIMARY KEY,
                    `messageId` TEXT NOT NULL,
                    `mimeType` TEXT NOT NULL,
                    `localUri` TEXT NOT NULL,
                    `remoteUrl` TEXT,
                    `storagePath` TEXT,
                    `thumbnailUrl` TEXT,
                    `fileName` TEXT NOT NULL,
                    `uploadStatus` TEXT NOT NULL DEFAULT 'PENDING'
                )
            """.trimIndent())
            
            try {
                val attachCols = mutableSetOf<String>()
                db.query("PRAGMA table_info(`attachments`)").use { cursor ->
                    val nameIdx = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        if (nameIdx >= 0) attachCols.add(cursor.getString(nameIdx))
                    }
                }
                if (!attachCols.contains("storagePath")) {
                    db.execSQL("ALTER TABLE `attachments` ADD COLUMN `storagePath` TEXT")
                }
                if (!attachCols.contains("uploadStatus")) {
                    db.execSQL("ALTER TABLE `attachments` ADD COLUMN `uploadStatus` TEXT NOT NULL DEFAULT 'PENDING'")
                }
            } catch (e: Exception) {
                android.util.Log.e("DB_MIGRATION", "Error checking columns for attachments", e)
            }

            // 4. Ensure memory_insights table exists
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `memory_insights` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `category` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL
                )
            """.trimIndent())

            // 5. Ensure archived_insights table exists
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `archived_insights` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `sessionId` TEXT NOT NULL,
                    `query` TEXT NOT NULL,
                    `introTitle` TEXT NOT NULL,
                    `jsonContent` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL
                )
            """.trimIndent())
        }

        val MIGRATION_1_6 = object : androidx.room.migration.Migration(1, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                migrateAnyTo6(db)
            }
        }
        val MIGRATION_2_6 = object : androidx.room.migration.Migration(2, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                migrateAnyTo6(db)
            }
        }
        val MIGRATION_3_6 = object : androidx.room.migration.Migration(3, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                migrateAnyTo6(db)
            }
        }
        val MIGRATION_4_6 = object : androidx.room.migration.Migration(4, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                migrateAnyTo6(db)
            }
        }
        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                migrateAnyTo6(db)
            }
        }

        fun isValidSqliteFile(file: java.io.File): Boolean {
            if (!file.exists() || !file.isFile || file.length() < 512) return false
            val name = file.name.lowercase()
            if (name.contains("-wal") || name.contains("-shm") || name.contains("-journal") ||
                name.endsWith(".wal") || name.endsWith(".shm") || name.endsWith(".journal")) {
                return false
            }
            return try {
                java.io.RandomAccessFile(file, "r").use { raf ->
                    if (raf.length() < 512) return false
                    val header = ByteArray(16)
                    raf.readFully(header)
                    val expected = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
                    if (!header.contentEquals(expected)) return false

                    raf.seek(16)
                    val pageSizeRaw = raf.readUnsignedShort()
                    val pageSize = if (pageSizeRaw == 1) 65536 else pageSizeRaw
                    pageSize in 512..65536 && (pageSize and (pageSize - 1)) == 0
                }
            } catch (e: Exception) {
                false
            }
        }

        @Volatile
        private var INSTANCE: DepthDatabase? = null

        fun getDatabase(context: android.content.Context): DepthDatabase {
            return INSTANCE ?: synchronized(this) {
                val dbFile = context.getDatabasePath("depthlens_database")
                val backupFile = java.io.File(context.filesDir, "depthlens_database.bak")
                
                // Safely preserve backups without overwriting non-empty backups with empty or invalid databases
                if (dbFile.exists() && dbFile.length() > 0 && isValidSqliteFile(dbFile)) {
                    try {
                        dbFile.copyTo(backupFile, overwrite = true)
                        val walFile = java.io.File(dbFile.path + "-wal")
                        if (walFile.exists() && walFile.length() > 0) {
                            val walBackup = java.io.File(context.filesDir, "depthlens_database-wal.bak")
                            walFile.copyTo(walBackup, overwrite = true)
                        }
                        val shmFile = java.io.File(dbFile.path + "-shm")
                        if (shmFile.exists() && shmFile.length() > 0) {
                            val shmBackup = java.io.File(context.filesDir, "depthlens_database-shm.bak")
                            shmFile.copyTo(shmBackup, overwrite = true)
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
                            try {
                                db.query("PRAGMA integrity_check").use { cursor ->
                                    if (cursor.moveToFirst()) {
                                        val res = cursor.getString(0)
                                        if (!res.equals("ok", ignoreCase = true)) {
                                            android.util.Log.w("DB_INTEGRITY", "Integrity check reported: $res")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    })
                    .addMigrations(MIGRATION_1_6, MIGRATION_2_6, MIGRATION_3_6, MIGRATION_4_6, MIGRATION_5_6)
                    .build()
                    
                    // Verify database opens
                    instance.openHelper.writableDatabase
                } catch (migrationEx: Exception) {
                    migrationEx.printStackTrace()
                    // If migration threw an unexpected exception, attempt restore with migrations preserved
                    if (backupFile.exists()) {
                        try {
                            instance?.close()
                        } catch (closeEx: Exception) {}
                        
                        try {
                            backupFile.copyTo(dbFile, overwrite = true)
                            val walBackup = java.io.File(context.filesDir, "depthlens_database-wal.bak")
                            val walFile = java.io.File(dbFile.path + "-wal")
                            if (walBackup.exists()) {
                                walBackup.copyTo(walFile, overwrite = true)
                            }
                            
                            instance = Room.databaseBuilder(
                                context.applicationContext,
                                DepthDatabase::class.java,
                                "depthlens_database"
                            )
                            .addMigrations(MIGRATION_1_6, MIGRATION_2_6, MIGRATION_3_6, MIGRATION_4_6, MIGRATION_5_6)
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
                .addMigrations(MIGRATION_1_6, MIGRATION_2_6, MIGRATION_3_6, MIGRATION_4_6, MIGRATION_5_6)
                .build()

                // Immediately run local backup recovery if database is currently empty
                try {
                    kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                        recoverFromLocalBackups(
                            context,
                            finalInstance.sessionDao(),
                            finalInstance.messageDao(),
                            finalInstance.attachmentDao()
                        )
                    }
                } catch (recEx: Exception) {
                    android.util.Log.e("DB_RECOVERY", "Auto-recovery on open error: ${recEx.message}", recEx)
                }
                
                INSTANCE = finalInstance
                finalInstance
            }
        }

        /**
         * Scans all backup files (.bak, .db, etc.) across the app directories to recover
         * and merge any historic sessions and messages lost during app updates.
         */
        suspend fun recoverFromLocalBackups(
            context: android.content.Context,
            sessionDao: SessionDao,
            messageDao: MessageDao,
            attachmentDao: AttachmentDao?
        ): Int {
            try {
                val prefs = context.getSharedPreferences("depthlens_prefs", android.content.Context.MODE_PRIVATE)
                val deletedSessionIds = prefs.getStringSet("deleted_session_ids_set", emptySet()) ?: emptySet()
                val currentSessions = sessionDao.getAllSessions()
                val existingSessionIds = currentSessions.map { it.id }.toMutableSet()
                android.util.Log.i("DB_RECOVERY", "Starting backup recovery scan. Currently active sessions: ${currentSessions.size}, deleted tombstones: ${deletedSessionIds.size}")

                val candidateFiles = mutableListOf<java.io.File>()
                val filesDir = context.filesDir
                val dbFile = context.getDatabasePath("depthlens_database")
                val dbDir = dbFile.parentFile
                val cacheDir = context.cacheDir
                val dataDir = try { java.io.File(context.applicationInfo.dataDir) } catch (e: Exception) { null }
                val extDir = try { context.getExternalFilesDir(null) } catch (e: Exception) { null }

                val appDatabasesDir = if (dataDir != null) java.io.File(dataDir, "databases") else null
                val appFilesDir = if (dataDir != null) java.io.File(dataDir, "files") else null

                // Clean up any lingering temp recovery files from cache
                try {
                    cacheDir.listFiles()?.forEach { f ->
                        if (f.name.startsWith("temp_recovery_")) {
                            try { f.delete() } catch (e: Exception) {}
                        }
                    }
                } catch (e: Exception) {}

                // Collect candidate backup files from all known directories
                val dirsToScan = listOfNotNull(filesDir, dbDir, cacheDir, dataDir, appDatabasesDir, appFilesDir, extDir)
                for (dir in dirsToScan) {
                    try {
                        if (dir.exists()) {
                            dir.walkTopDown().maxDepth(4).forEach { f ->
                                if (f.isFile && f.length() >= 512) {
                                    val name = f.name.lowercase()
                                    val isWalOrShmOrJournal = name.contains("-wal") ||
                                            name.contains("-shm") ||
                                            name.contains("-journal") ||
                                            name.endsWith(".wal") ||
                                            name.endsWith(".shm") ||
                                            name.endsWith(".journal")
                                    val isTemp = f.name.startsWith("temp_recovery_") || isWalOrShmOrJournal
                                    val isDbCandidate = (name.endsWith(".bak") ||
                                            name.endsWith(".backup") ||
                                            name.endsWith(".db") ||
                                            name.endsWith(".sqlite") ||
                                            name.contains("database") ||
                                            name.contains("depthlens")) && !isTemp
                                    val isCurrentLiveDb = try { f.canonicalPath == dbFile.canonicalPath } catch (e: Exception) { false }
                                    if (isDbCandidate && !isCurrentLiveDb && isValidSqliteFile(f) && !candidateFiles.contains(f)) {
                                        candidateFiles.add(f)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignore directory walk error
                    }
                }

                android.util.Log.i("DB_RECOVERY", "Found ${candidateFiles.size} candidate database files to inspect")

                var newlyRestoredCount = 0

                // Custom database error handler that prevents deleting recovery candidates
                val safeErrorHandler = android.database.DatabaseErrorHandler { dbObj ->
                    android.util.Log.w("DB_RECOVERY", "Non-fatal corruption notice caught during inspection for: ${dbObj?.path}")
                }

                for (bFile in candidateFiles) {
                    if (!bFile.exists() || !isValidSqliteFile(bFile)) continue
                    android.util.Log.i("DB_RECOVERY", "Inspecting backup candidate: ${bFile.name} (${bFile.length()} bytes)")
                    
                    val tempDb = java.io.File(context.cacheDir, "temp_recovery_${System.currentTimeMillis()}_${bFile.name.hashCode()}.db")
                    val tempWal = java.io.File(tempDb.path + "-wal")
                    val tempShm = java.io.File(tempDb.path + "-shm")
                    
                    var backupDb: android.database.sqlite.SQLiteDatabase? = null
                    try {
                        bFile.copyTo(tempDb, overwrite = true)
                        
                        // Locate matching WAL file across multiple naming schemes
                        val potentialWal = listOfNotNull(
                            java.io.File(bFile.parentFile, bFile.name + "-wal").takeIf { it.exists() && it.length() > 0 },
                            java.io.File(bFile.parentFile, bFile.nameWithoutExtension + "-wal").takeIf { it.exists() && it.length() > 0 },
                            java.io.File(bFile.parentFile, bFile.nameWithoutExtension + "-wal.bak").takeIf { it.exists() && it.length() > 0 },
                            java.io.File(bFile.parentFile, bFile.name + ".wal").takeIf { it.exists() && it.length() > 0 },
                            java.io.File(filesDir, "depthlens_database-wal.bak").takeIf { it.exists() && it.length() > 0 },
                            java.io.File(filesDir, "depthlens_database-wal").takeIf { it.exists() && it.length() > 0 },
                            if (dbDir != null) java.io.File(dbDir, "depthlens_database-wal").takeIf { it.exists() && it.length() > 0 } else null
                        ).firstOrNull()

                        potentialWal?.copyTo(tempWal, overwrite = true)

                        // Locate matching SHM file
                        val potentialShm = listOfNotNull(
                            java.io.File(bFile.parentFile, bFile.name + "-shm").takeIf { it.exists() && it.length() > 0 },
                            java.io.File(bFile.parentFile, bFile.nameWithoutExtension + "-shm").takeIf { it.exists() && it.length() > 0 },
                            java.io.File(bFile.parentFile, bFile.nameWithoutExtension + "-shm.bak").takeIf { it.exists() && it.length() > 0 },
                            java.io.File(filesDir, "depthlens_database-shm.bak").takeIf { it.exists() && it.length() > 0 },
                            java.io.File(filesDir, "depthlens_database-shm").takeIf { it.exists() && it.length() > 0 },
                            if (dbDir != null) java.io.File(dbDir, "depthlens_database-shm").takeIf { it.exists() && it.length() > 0 } else null
                        ).firstOrNull()

                        potentialShm?.copyTo(tempShm, overwrite = true)

                        // Crucial: Open with safe error handler on temp file
                        val openFlags = android.database.sqlite.SQLiteDatabase.OPEN_READONLY or
                                android.database.sqlite.SQLiteDatabase.NO_LOCALIZED_COLLATORS
                        backupDb = try {
                            android.database.sqlite.SQLiteDatabase.openDatabase(
                                tempDb.absolutePath,
                                null,
                                openFlags,
                                safeErrorHandler
                            )
                        } catch (corruptEx: android.database.sqlite.SQLiteDatabaseCorruptException) {
                            android.util.Log.w("DB_RECOVERY", "Candidate backup ${bFile.name} is corrupt: ${corruptEx.message}")
                            try { bFile.delete() } catch (e: Exception) {}
                            null
                        } catch (openEx: Exception) {
                            android.util.Log.w("DB_RECOVERY", "Could not open candidate db ${bFile.name}: ${openEx.message}")
                            null
                        }

                        if (backupDb == null) {
                            continue
                        }

                        // Checkpoint any WAL frames directly into the main SQLite database tables
                        try {
                            backupDb.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
                        } catch (cpEx: Exception) {
                            // Ignored if WAL not enabled or read-only
                        }
                        
                        // Find all non-system tables in SQLite master
                        val allTables = mutableListOf<String>()
                        backupDb.rawQuery(
                            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' AND name NOT LIKE 'room_%'",
                            null
                        ).use { cursor ->
                            while (cursor.moveToNext()) {
                                allTables.add(cursor.getString(0))
                            }
                        }

                        val sessionTableCandidates = listOf("sessions", "chats", "conversations", "chat_sessions", "chatHistory")
                        var sessionTableName: String? = sessionTableCandidates.firstOrNull { allTables.contains(it) }
                        if (sessionTableName == null) {
                            // Inspect table columns to locate session table dynamically
                            for (tbl in allTables) {
                                val cols = mutableSetOf<String>()
                                try {
                                    backupDb.rawQuery("PRAGMA table_info(`$tbl`)", null).use { tc ->
                                        val nIdx = tc.getColumnIndex("name")
                                        while (tc.moveToNext()) {
                                            if (nIdx >= 0) cols.add(tc.getString(nIdx).lowercase())
                                        }
                                    }
                                    if (cols.contains("id") && (cols.contains("title") || cols.contains("name") || cols.contains("topic"))) {
                                        sessionTableName = tbl
                                        break
                                    }
                                } catch (e: Exception) {}
                            }
                        }
                        
                        if (sessionTableName == null) {
                            android.util.Log.d("DB_RECOVERY", "No session table found in ${bFile.name}, available tables: $allTables")
                            continue
                        }
                        
                        val sessionEntities = mutableListOf<com.example.data.model.SessionEntity>()
                        backupDb.rawQuery("SELECT * FROM `$sessionTableName`", null).use { cursor ->
                            val idIdx = cursor.getColumnIndex("id").takeIf { it >= 0 } ?: cursor.getColumnIndex("sessionId")
                            val titleIdx = cursor.getColumnIndex("title").takeIf { it >= 0 } ?: cursor.getColumnIndex("name")
                            val createdIdx = cursor.getColumnIndex("createdAt").takeIf { it >= 0 } ?: cursor.getColumnIndex("timestamp")
                            val updatedIdx = cursor.getColumnIndex("lastUpdatedAt").takeIf { it >= 0 } ?: cursor.getColumnIndex("updatedAt")
                            val pinnedIdx = cursor.getColumnIndex("isPinned").takeIf { it >= 0 } ?: cursor.getColumnIndex("pinned")
                            
                            while (cursor.moveToNext()) {
                                val sId = if (idIdx != null && idIdx >= 0) cursor.getString(idIdx) else null
                                if (sId.isNullOrBlank()) continue
                                val sTitle = if (titleIdx != null && titleIdx >= 0) cursor.getString(titleIdx).orEmpty() else "Chat"
                                val cAt = if (createdIdx != null && createdIdx >= 0 && !cursor.isNull(createdIdx)) cursor.getLong(createdIdx) else System.currentTimeMillis()
                                val uAt = if (updatedIdx != null && updatedIdx >= 0 && !cursor.isNull(updatedIdx)) cursor.getLong(updatedIdx) else cAt
                                val isPinned = if (pinnedIdx != null && pinnedIdx >= 0 && !cursor.isNull(pinnedIdx)) cursor.getInt(pinnedIdx) == 1 else false
                                
                                sessionEntities.add(
                                    com.example.data.model.SessionEntity(
                                        id = sId,
                                        title = if (sTitle.isBlank()) "Chat" else sTitle,
                                        createdAt = cAt,
                                        lastUpdatedAt = uAt,
                                        isPinned = isPinned
                                    )
                                )
                            }
                        }
                        
                        if (sessionEntities.isNotEmpty()) {
                            android.util.Log.i("DB_RECOVERY", "Extracted ${sessionEntities.size} sessions from ${bFile.name}!")
                            for (session in sessionEntities) {
                                if (deletedSessionIds.contains(session.id)) {
                                    continue
                                }
                                if (!existingSessionIds.contains(session.id)) {
                                    sessionDao.insertSession(session)
                                    existingSessionIds.add(session.id)
                                    newlyRestoredCount++
                                } else {
                                    // If title in active database was empty or generic, update it
                                    if (session.title.isNotBlank() && session.title != "Chat" && session.title != "New Chat") {
                                        sessionDao.renameSession(session.id, session.title)
                                    }
                                }
                            }
                            
                            // Check for messages table
                            val messageTableCandidates = listOf("messages", "chat_messages", "chats", "history", "chatHistory")
                            var messageTableName: String? = messageTableCandidates.firstOrNull { allTables.contains(it) && it != sessionTableName }
                            if (messageTableName == null) {
                                for (tbl in allTables) {
                                    if (tbl == sessionTableName) continue
                                    val cols = mutableSetOf<String>()
                                    try {
                                        backupDb.rawQuery("PRAGMA table_info(`$tbl`)", null).use { tc ->
                                            val nIdx = tc.getColumnIndex("name")
                                            while (tc.moveToNext()) {
                                                if (nIdx >= 0) cols.add(tc.getString(nIdx).lowercase())
                                            }
                                        }
                                        if ((cols.contains("id") || cols.contains("messageid")) && (cols.contains("sessionid") || cols.contains("chatid")) && (cols.contains("text") || cols.contains("content"))) {
                                            messageTableName = tbl
                                            break
                                        }
                                    } catch (e: Exception) {}
                                }
                            }
                            if (messageTableName == null && allTables.contains("messages")) {
                                messageTableName = "messages"
                            }
                            
                            if (messageTableName != null) {
                                val messageEntities = mutableListOf<com.example.data.model.MessageEntity>()
                                backupDb.rawQuery("SELECT * FROM `$messageTableName`", null).use { cursor ->
                                    val idIdx = cursor.getColumnIndex("id").takeIf { it >= 0 } ?: cursor.getColumnIndex("messageId")
                                    val sIdIdx = cursor.getColumnIndex("sessionId").takeIf { it >= 0 } ?: cursor.getColumnIndex("chatId")
                                    val roleIdx = cursor.getColumnIndex("role").takeIf { it >= 0 } ?: cursor.getColumnIndex("sender")
                                    val textIdx = cursor.getColumnIndex("text").takeIf { it >= 0 } ?: cursor.getColumnIndex("content")
                                    val imgIdx = cursor.getColumnIndex("imageUri").takeIf { it >= 0 } ?: cursor.getColumnIndex("imageUrl")
                                    val timeIdx = cursor.getColumnIndex("timestamp").takeIf { it >= 0 } ?: cursor.getColumnIndex("time")
                                    val replyIdx = cursor.getColumnIndex("replyToMessageId")
                                    val selIdx = cursor.getColumnIndex("selectedText")
                                    
                                    while (cursor.moveToNext()) {
                                        val mId = if (idIdx != null && idIdx >= 0) cursor.getString(idIdx) else null
                                        val sId = if (sIdIdx != null && sIdIdx >= 0) cursor.getString(sIdIdx) else null
                                        if (mId.isNullOrBlank() || sId.isNullOrBlank() || deletedSessionIds.contains(sId)) continue
                                        val role = if (roleIdx != null && roleIdx >= 0) cursor.getString(roleIdx).orEmpty() else "user"
                                        val text = if (textIdx != null && textIdx >= 0) cursor.getString(textIdx).orEmpty() else ""
                                        val img = if (imgIdx != null && imgIdx >= 0 && !cursor.isNull(imgIdx)) cursor.getString(imgIdx) else null
                                        val time = if (timeIdx != null && timeIdx >= 0 && !cursor.isNull(timeIdx)) cursor.getLong(timeIdx) else System.currentTimeMillis()
                                        val reply = if (replyIdx != null && replyIdx >= 0 && !cursor.isNull(replyIdx)) cursor.getString(replyIdx) else null
                                        val sel = if (selIdx != null && selIdx >= 0 && !cursor.isNull(selIdx)) cursor.getString(selIdx) else null
                                        
                                        messageEntities.add(
                                            com.example.data.model.MessageEntity(
                                                id = mId,
                                                sessionId = sId,
                                                role = role,
                                                text = text,
                                                imageUri = img,
                                                timestamp = time,
                                                replyToMessageId = reply,
                                                selectedText = sel
                                            )
                                        )
                                    }
                                }
                                
                                android.util.Log.i("DB_RECOVERY", "Restoring ${messageEntities.size} messages from ${bFile.name}...")
                                for (msg in messageEntities) {
                                    messageDao.insertMessage(msg)
                                }
                            }
                            
                            if (attachmentDao != null && allTables.contains("attachments")) {
                                backupDb.rawQuery("SELECT * FROM attachments", null).use { cursor ->
                                    val aIdIdx = cursor.getColumnIndex("attachmentId")
                                    val mIdIdx = cursor.getColumnIndex("messageId")
                                    val mimeIdx = cursor.getColumnIndex("mimeType")
                                    val locIdx = cursor.getColumnIndex("localUri")
                                    val remIdx = cursor.getColumnIndex("remoteUrl")
                                    val pathIdx = cursor.getColumnIndex("storagePath")
                                    val thumbIdx = cursor.getColumnIndex("thumbnailUrl")
                                    val nameIdx = cursor.getColumnIndex("fileName")
                                    val statIdx = cursor.getColumnIndex("uploadStatus")
                                    
                                    while (cursor.moveToNext()) {
                                        val aId = if (aIdIdx >= 0) cursor.getString(aIdIdx) else null
                                        val mId = if (mIdIdx >= 0) cursor.getString(mIdIdx) else null
                                        if (aId.isNullOrBlank() || mId.isNullOrBlank()) continue
                                        attachmentDao.insertAttachment(
                                            com.example.data.model.AttachmentEntity(
                                                attachmentId = aId,
                                                messageId = mId,
                                                mimeType = if (mimeIdx >= 0) cursor.getString(mimeIdx).orEmpty() else "application/octet-stream",
                                                localUri = if (locIdx >= 0) cursor.getString(locIdx).orEmpty() else "",
                                                remoteUrl = if (remIdx >= 0) cursor.getString(remIdx) else null,
                                                storagePath = if (pathIdx >= 0) cursor.getString(pathIdx) else null,
                                                thumbnailUrl = if (thumbIdx >= 0) cursor.getString(thumbIdx) else null,
                                                fileName = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else "file",
                                                uploadStatus = if (statIdx >= 0) cursor.getString(statIdx).orEmpty() else "PENDING"
                                            )
                                        )
                                    }
                                }
                            }
                            
                            try {
                                val permanentArchive = java.io.File(filesDir, "depthlens_database_recovered_archive.bak")
                                if (!permanentArchive.exists()) {
                                    bFile.copyTo(permanentArchive, overwrite = true)
                                }
                            } catch (e: Exception) {}
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DB_RECOVERY", "Error checking backup file ${bFile.name}: ${e.message}", e)
                    } finally {
                        try { backupDb?.close() } catch (e: Exception) {}
                        try { tempDb.delete() } catch (e: Exception) {}
                        try { tempWal.delete() } catch (e: Exception) {}
                        try { tempShm.delete() } catch (e: Exception) {}
                    }
                }

                android.util.Log.i("DB_RECOVERY", "Backup recovery finished. Restored sessions count: $newlyRestoredCount")
                return newlyRestoredCount
            } catch (e: Exception) {
                android.util.Log.e("DB_RECOVERY", "Fatal exception in recoverFromLocalBackups", e)
                return 0
            }
        }
    }
}