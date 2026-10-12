package io.jacob.episodive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 라이브 스테이션 즐겨찾기(`favorite_stations`)와 최근 청취(`recent_stations`) 표를 더한다.
 *
 * 기존 표는 건드리지 않는 순수 추가다. DDL 은 Room 이 내보낸 `schemas/…/13.json` 의 `createSql`
 * 을 그대로 옮긴 것이다 — 한 글자라도 어긋나면 실행 시점의 스키마 검증이 실패한다.
 */
val Migration12to13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `favorite_stations` (" +
                "`stationId` TEXT NOT NULL, `favoritedAtMillis` INTEGER NOT NULL, " +
                "PRIMARY KEY(`stationId`))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_favorite_stations_favoritedAtMillis` " +
                "ON `favorite_stations` (`favoritedAtMillis`)"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recent_stations` (" +
                "`stationId` TEXT NOT NULL, `playedAtMillis` INTEGER NOT NULL, " +
                "PRIMARY KEY(`stationId`))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_recent_stations_playedAtMillis` " +
                "ON `recent_stations` (`playedAtMillis`)"
        )
    }
}
