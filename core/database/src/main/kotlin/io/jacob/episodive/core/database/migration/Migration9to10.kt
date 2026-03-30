package io.jacob.episodive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val Migration9to10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. saved_episodes 테이블 생성
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS saved_episodes (
                id INTEGER NOT NULL PRIMARY KEY,
                podcastId INTEGER NOT NULL,
                savedAt INTEGER NOT NULL,
                filePath TEXT NOT NULL,
                totalSize INTEGER NOT NULL,
                downloadedSize INTEGER NOT NULL,
                downloadStatus TEXT NOT NULL,
                FOREIGN KEY (id) REFERENCES episodes(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // 2. savedAt 인덱스 생성
        db.execSQL("CREATE INDEX IF NOT EXISTS index_saved_episodes_savedAt ON saved_episodes (savedAt)")

        // 3. 기존 view 삭제
        db.execSQL("DROP VIEW IF EXISTS episode_with_extras")

        // 4. 새 view 생성 (saved_episodes LEFT JOIN 포함)
        db.execSQL(
            """
            CREATE VIEW episode_with_extras AS SELECT
                episodes.*,
                liked_episodes.likedAt AS likedAt,
                played_episodes.playedAt AS playedAt,
                played_episodes.position AS position,
                played_episodes.isCompleted AS isCompleted,
                soundbites.startTime AS clipStartTime,
                soundbites.duration AS clipDuration,
                saved_episodes.savedAt AS savedAt,
                saved_episodes.filePath AS filePath,
                saved_episodes.downloadStatus AS downloadStatus
            FROM episodes
            LEFT JOIN liked_episodes ON episodes.id = liked_episodes.id
            LEFT JOIN played_episodes ON episodes.id = played_episodes.id
            LEFT JOIN soundbites ON episodes.id = soundbites.episodeId
            LEFT JOIN saved_episodes ON episodes.id = saved_episodes.id
            """.trimIndent()
        )
    }
}
