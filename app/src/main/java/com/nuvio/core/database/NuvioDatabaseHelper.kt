package com.nuvio.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class NuvioDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Table: download_tasks
        db.execSQL(
            """
            CREATE TABLE download_tasks (
                id TEXT PRIMARY KEY NOT NULL,
                url TEXT NOT NULL,
                title TEXT NOT NULL,
                filePath TEXT NOT NULL,
                mimeType TEXT NOT NULL,
                totalBytes INTEGER NOT NULL,
                downloadedBytes INTEGER NOT NULL,
                status TEXT NOT NULL,
                supportsRanges INTEGER NOT NULL,
                etag TEXT,
                lastModified TEXT,
                connectionCount INTEGER NOT NULL,
                downloadSpeed INTEGER NOT NULL,
                isMediaExtraction INTEGER NOT NULL,
                formatId TEXT,
                thumbnailUri TEXT,
                errorMessage TEXT,
                createdAt INTEGER NOT NULL,
                completedAt INTEGER
            );
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX index_download_tasks_status ON download_tasks(status);")
        db.execSQL("CREATE INDEX index_download_tasks_createdAt ON download_tasks(createdAt);")

        // Table: download_segments
        db.execSQL(
            """
            CREATE TABLE download_segments (
                id TEXT PRIMARY KEY NOT NULL,
                taskId TEXT NOT NULL,
                segmentIndex INTEGER NOT NULL,
                startByte INTEGER NOT NULL,
                endByte INTEGER NOT NULL,
                downloadedBytes INTEGER NOT NULL,
                status TEXT NOT NULL,
                FOREIGN KEY (taskId) REFERENCES download_tasks(id) ON DELETE CASCADE
            );
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX index_download_segments_taskId ON download_segments(taskId);")

        // Table: playback_history
        db.execSQL(
            """
            CREATE TABLE playback_history (
                id TEXT PRIMARY KEY NOT NULL,
                mediaUri TEXT UNIQUE NOT NULL,
                title TEXT NOT NULL,
                artist TEXT,
                durationMs INTEGER NOT NULL,
                playbackPositionMs INTEGER NOT NULL,
                isVideo INTEGER NOT NULL,
                lastPlayedAt INTEGER NOT NULL
            );
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX index_playback_history_lastPlayedAt ON playback_history(lastPlayedAt);")

        // Table: browser_bookmarks
        db.execSQL(
            """
            CREATE TABLE browser_bookmarks (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                url TEXT NOT NULL,
                faviconUri TEXT,
                createdAt INTEGER NOT NULL
            );
            """.trimIndent()
        )

        // Table: browser_history
        db.execSQL(
            """
            CREATE TABLE browser_history (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                url TEXT NOT NULL,
                visitedAt INTEGER NOT NULL
            );
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version migrations
    }

    companion object {
        const val DATABASE_NAME = "nuvio.db"
        const val DATABASE_VERSION = 1
    }
}
