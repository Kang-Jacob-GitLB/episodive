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
        // @DatabaseView 어노테이션이 생성하는 SQL과 정확히 일치해야 함 (백틱, 들여쓰기)
        db.execSQL(
            "CREATE VIEW `episode_with_extras` AS SELECT\n" +
                "            episodes.*,\n" +
                "            liked_episodes.likedAt AS likedAt,\n" +
                "            played_episodes.playedAt AS playedAt,\n" +
                "            played_episodes.position AS position,\n" +
                "            played_episodes.isCompleted AS isCompleted,\n" +
                "            soundbites.startTime AS clipStartTime,\n" +
                "            soundbites.duration AS clipDuration,\n" +
                "            saved_episodes.savedAt AS savedAt,\n" +
                "            saved_episodes.filePath AS filePath,\n" +
                "            saved_episodes.downloadStatus AS downloadStatus\n" +
                "        FROM episodes\n" +
                "        LEFT JOIN liked_episodes ON episodes.id = liked_episodes.id\n" +
                "        LEFT JOIN played_episodes ON episodes.id = played_episodes.id\n" +
                "        LEFT JOIN soundbites ON episodes.id = soundbites.episodeId\n" +
                "        LEFT JOIN saved_episodes ON episodes.id = saved_episodes.id"
        )
    }
}
