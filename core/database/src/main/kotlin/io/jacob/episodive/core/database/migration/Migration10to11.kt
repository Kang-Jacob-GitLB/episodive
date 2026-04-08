package io.jacob.episodive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val Migration10to11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP VIEW IF EXISTS episode_with_extras")

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
                "            saved_episodes.downloadStatus AS downloadStatus,\n" +
                "            saved_episodes.downloadedSize AS downloadedSize,\n" +
                "            saved_episodes.totalSize AS totalSize\n" +
                "        FROM episodes\n" +
                "        LEFT JOIN liked_episodes ON episodes.id = liked_episodes.id\n" +
                "        LEFT JOIN played_episodes ON episodes.id = played_episodes.id\n" +
                "        LEFT JOIN soundbites ON episodes.id = soundbites.episodeId\n" +
                "        LEFT JOIN saved_episodes ON episodes.id = saved_episodes.id"
        )
    }
}
