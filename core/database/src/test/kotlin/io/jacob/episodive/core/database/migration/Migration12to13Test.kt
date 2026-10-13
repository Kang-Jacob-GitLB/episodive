package io.jacob.episodive.core.database.migration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.jacob.episodive.core.database.EpisodiveDatabase
import io.jacob.episodive.core.database.dao.recentStationEntityOf
import io.jacob.episodive.core.database.dao.stationIdsTestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * v12 → v13 마이그레이션 검증. 즐겨찾기·최근 청취 표을 더하는 순수 추가다.
 *
 * [Migration11to12Test] 와 같은 방식이다. Room 이 내보낸 `schemas/…/12.json` 으로 v12 DB 파일을
 * 만들어 두고 [EpisodiveDatabase] 를 열면, Room 이 마이그레이션을 돌린 뒤 결과가 v13 정의와
 * 같은지 직접 검증한다. 어긋나면 예외로 죽는다.
 */
@RunWith(RobolectricTestRunner::class)
class Migration12to13Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    @After
    fun deleteTestDatabase() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun `Given a v12 database, When opened at v13, Then the schema Room expects is produced`() = runTest {
        createDatabaseAt(context, TEST_DB, version = 12) { }

        val db = openAtCurrentVersion()

        try {
            // 열리는 것 자체가 스키마 검증 통과다. 새 표가 실제로 쓰이는지도 본다.
            val dao = db.stationDao()
            assertEquals(emptyList<String>(), dao.getFavoriteStationIds().first())
            assertEquals(emptyList<String>(), dao.getRecentStationIds().first())

            assertTrue(dao.toggleFavorite(stationIdsTestData.first()))
            dao.upsertRecent(recentStationEntityOf(stationIdsTestData.first()))
            assertEquals(listOf(stationIdsTestData.first()), dao.getFavoriteStationIds().first())
            assertEquals(listOf(stationIdsTestData.first()), dao.getRecentStationIds().first())
        } finally {
            db.close()
        }
    }

    @Test
    fun `Given a v12 database with a feed, When opened at v13, Then existing data survives`() {
        createDatabaseAt(context, TEST_DB, version = 12) { db ->
            db.execSQL(
                "INSERT INTO `feeds` (`id`, `url`, `title`, `newestItemPublishTime`, " +
                    "`description`, `image`, `itunesId`, `language`, `categories`, `groupKey`, " +
                    "`cachedAt`, `sortOrder`) VALUES (11, 'https://example.com/11', 'Feed 11', " +
                    "2000, NULL, NULL, NULL, 'en', '', 'trending', 1000, 0)"
            )
        }

        val db = openAtCurrentVersion()

        try {
            val feeds = db.feedDao().getFeeds(groupKey = "trending", limit = 10)
            assertEquals(listOf(11L), feeds.map { it.id })
        } finally {
            db.close()
        }
    }

    private fun openAtCurrentVersion(): EpisodiveDatabase =
        Room.databaseBuilder(context, EpisodiveDatabase::class.java, TEST_DB)
            .addMigrations(Migration12to13)
            .allowMainThreadQueries()
            .build()

    companion object {
        private const val TEST_DB = "migration-test.db"
    }
}
