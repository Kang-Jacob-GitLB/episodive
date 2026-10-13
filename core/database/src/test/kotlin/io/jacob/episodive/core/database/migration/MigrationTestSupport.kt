package io.jacob.episodive.core.database.migration

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.json.JSONObject
import java.io.File

// 마이그레이션 테스트가 함께 쓰는 헬퍼. Room 이 내보낸 `schemas/…/<버전>.json` 으로 옛 버전 DB 를 만든다.

/**
 * Room 이 내보낸 스키마 정의대로 [version] 시점의 DB 파일을 만든다.
 *
 * `MigrationTestHelper` 를 쓰지 않는 이유는 그쪽이 스키마를 계측 테스트의 asset 에서 찾기
 * 때문이다. 이 모듈의 테스트는 Robolectric 로컬 테스트라 파일에서 직접 읽는 편이 단순하다.
 */
internal fun createDatabaseAt(
    context: Context,
    dbName: String,
    version: Int,
    populate: (SupportSQLiteDatabase) -> Unit,
) {
    val schema = JSONObject(schemaFile(version).readText()).getJSONObject("database")

    val callback = object : SupportSQLiteOpenHelper.Callback(version) {
        override fun onCreate(db: SupportSQLiteDatabase) {
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val tableName = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").withTableName(tableName))

                entity.optJSONArray("indices")?.let { indices ->
                    for (j in 0 until indices.length()) {
                        db.execSQL(
                            indices.getJSONObject(j).getString("createSql")
                                .withTableName(tableName)
                        )
                    }
                }
                entity.optJSONArray("contentSyncTriggers")?.let { triggers ->
                    for (j in 0 until triggers.length()) {
                        db.execSQL(triggers.getString(j))
                    }
                }
            }

            val views = schema.optJSONArray("views")
            for (i in 0 until (views?.length() ?: 0)) {
                val view = views!!.getJSONObject(i)
                db.execSQL(
                    view.getString("createSql")
                        .replace("\${VIEW_NAME}", view.getString("viewName"))
                )
            }

            // room_master_table 과 신원 해시. 이게 없으면 Room 이 "데이터 무결성을 확인할 수
            // 없다"며 마이그레이션을 시작하지도 않는다.
            val setupQueries = schema.getJSONArray("setupQueries")
            for (i in 0 until setupQueries.length()) {
                db.execSQL(setupQueries.getString(i))
            }

            populate(db)
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
            Unit
    }

    val helper = FrameworkSQLiteOpenHelperFactory().create(
        SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(callback)
            .build()
    )
    helper.writableDatabase.close()
}

private fun String.withTableName(tableName: String) =
    replace("\${TABLE_NAME}", tableName)

private fun schemaFile(version: Int) = File(
    "schemas/io.jacob.episodive.core.database.EpisodiveDatabase/$version.json"
).also {
    check(it.exists()) { "스키마 파일을 찾지 못했다: ${it.absolutePath}" }
}
