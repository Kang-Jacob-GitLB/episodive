package io.jacob.episodive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Radio Browser 스테이션 메타 표(`radio_browser_stations`)를 더한다.
 *
 * 기존 표는 건드리지 않는 순수 추가다. DDL 은 Room 이 내보낸 `schemas/…/14.json` 의 `createSql`
 * 을 그대로 옮긴 것이다 — 한 글자라도 어긋나면 실행 시점의 스키마 검증이 실패한다.
 */
val Migration13to14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `radio_browser_stations` (" +
                "`stationId` TEXT NOT NULL, `uuid` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`region` TEXT, `language` TEXT, `isHls` INTEGER NOT NULL, " +
                "`savedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`stationId`))"
        )
    }
}
