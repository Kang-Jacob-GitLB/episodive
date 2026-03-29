package io.jacob.episodive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val Migration8to9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 기존 테이블 rename
        db.execSQL("ALTER TABLE recent_searches RENAME TO recent_searches_old")

        // 새 테이블 생성
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS recent_searches (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                type TEXT NOT NULL,
                query TEXT,
                contentId INTEGER,
                title TEXT,
                imageUrl TEXT,
                subtitle TEXT,
                searchedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // unique index 생성
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_recent_searches_type_contentId ON recent_searches (type, contentId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_recent_searches_type_query ON recent_searches (type, query)")

        // 데이터 이전
        db.execSQL(
            """
            INSERT INTO recent_searches (type, query, searchedAt)
            SELECT 'query', `query`, searchedAt FROM recent_searches_old
            """.trimIndent()
        )

        // 이전 테이블 삭제
        db.execSQL("DROP TABLE recent_searches_old")
    }
}
