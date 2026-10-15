package io.jacob.episodive.core.database.migration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.jacob.episodive.core.database.EpisodiveDatabase
import io.jacob.episodive.core.database.dao.favoriteStationEntityOf
import io.jacob.episodive.core.database.dao.radioBrowserStationEntityOf
import io.jacob.episodive.core.database.dao.remoteStationIdsTestData
import io.jacob.episodive.core.database.dao.stationIdsTestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * v13 → v14 마이그레이션 검증. Radio Browser 스테이션 메타 표를 더하는 순수 추가다.
 *
 * [Migration12to13Test] 와 같은 방식이다. Room 이 내보낸 `schemas/…/13.json` 으로 v13 DB 파일을
 * 만들어 두고 [EpisodiveDatabase] 를 열면, Room 이 마이그레이션을 돌린 뒤 결과가 v14 정의와
 * 같은지 직접 검증한다. 어긋나면 예외로 죽는다.
 */
@RunWith(RobolectricTestRunner::class)
class Migration13to14Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    @After
    fun deleteTestDatabase() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun `Given a v13 database, When opened at v14, Then the schema Room expects is produced`() = runTest {
        createDatabaseAt(context, TEST_DB, version = 13) { }

        val db = openAtCurrentVersion()

        try {
            // 열리는 것 자체가 스키마 검증 통과다. 새 표가 실제로 쓰이는지도 본다.
            val dao = db.stationDao()
            val remoteId = remoteStationIdsTestData.first()
            assertEquals(emptyList<String>(), dao.getFavoriteStationsWithMeta().first().map { it.stationId })

            dao.upsertRemoteStation(radioBrowserStationEntityOf(remoteId))
            dao.addFavorite(favoriteStationEntityOf(remoteId))

            assertEquals(remoteId, dao.getFavoriteStationsWithMeta().first().single().meta?.stationId)
        } finally {
            db.close()
        }
    }

    @Test
    fun `Given a v13 database with a favorite, When opened at v14, Then existing data survives`() = runTest {
        val stationId = stationIdsTestData.first()
        createDatabaseAt(context, TEST_DB, version = 13) { db ->
            db.execSQL(
                "INSERT INTO `favorite_stations` (`stationId`, `favoritedAtMillis`) " +
                    "VALUES ('$stationId', 1000)"
            )
        }

        val db = openAtCurrentVersion()

        try {
            val favorite = db.stationDao().getFavoriteStationsWithMeta().first().single()
            assertEquals(stationId, favorite.stationId)
            assertEquals(null, favorite.meta)
        } finally {
            db.close()
        }
    }

    private fun openAtCurrentVersion(): EpisodiveDatabase =
        Room.databaseBuilder(context, EpisodiveDatabase::class.java, TEST_DB)
            .addMigrations(Migration13to14)
            .allowMainThreadQueries()
            .build()

    companion object {
        private const val TEST_DB = "migration-test.db"
    }
}
