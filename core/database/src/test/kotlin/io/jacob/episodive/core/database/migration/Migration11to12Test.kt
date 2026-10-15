package io.jacob.episodive.core.database.migration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.jacob.episodive.core.database.EpisodiveDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * v11 → v12 마이그레이션 검증.
 *
 * 이 마이그레이션은 이 앱에서 처음으로 **테이블을 다시 만든다**(`feeds` 의 기본키 변경). 컬럼
 * 추가와 달리 DDL 을 손으로 적어야 하고, 한 글자만 어긋나도 업그레이드한 기기에서 앱이 열리다
 * 죽는다 — Room 이 여는 순간 스키마를 검증하기 때문이다.
 *
 * 그 검증을 그대로 이용한다. v11 스키마로 DB 파일을 만들어 두고 [EpisodiveDatabase] 를 열면,
 * Room 이 마이그레이션을 돌린 뒤 결과가 v12 정의와 같은지 직접 확인한다. 어긋나면 이 테스트가
 * 예외로 죽는다. 스키마 정의는 Room 이 내보낸 `schemas/…/11.json` 을 읽어 쓰므로, 사람이 옮겨
 * 적다가 틀릴 여지가 없다.
 */
@RunWith(RobolectricTestRunner::class)
class Migration11to12Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    @After
    fun deleteTestDatabase() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun `Given a v11 database, When opened at v12, Then the schema Room expects is produced`() {
        createV11DatabaseWithOneFeedAndOneSoundbite()

        val db = openAtCurrentVersion()

        try {
            // 열리는 것 자체가 스키마 검증 통과다. 이어서 옮겨온 데이터를 확인한다.
            val feeds = db.feedDao().getFeeds(groupKey = "trending", limit = 10)
            assertEquals(1, feeds.size)
            assertEquals(11L, feeds.first().id)
            assertEquals("Feed 11", feeds.first().title)
            // 옛 행에는 순위 정보가 없다. 기본값 0 이 들어가고, 쿼리의 동점 처리가 순서를 잡는다.
            assertEquals(0, feeds.first().sortOrder)
        } finally {
            db.close()
        }
    }

    @Test
    fun `Given a migrated database, When the same feed is stored in another group, Then both survive`() = runTest {
        // 복합키가 실제로 적용됐는지 본다. DDL 만 맞고 PRIMARY KEY 절이 빠지면 위 테스트는
        // 통과할 수 있어도(컬럼 구성은 같으므로) 이 테스트는 통과할 수 없다.
        createV11DatabaseWithOneFeedAndOneSoundbite()

        val db = openAtCurrentVersion()

        try {
            val dao = db.feedDao()
            val migrated = dao.getFeeds(groupKey = "trending", limit = 10).first()
            dao.upsertFeeds(listOf(migrated.copy(groupKey = "recommended")))

            assertEquals(1, dao.getFeeds(groupKey = "trending", limit = 10).size)
            assertEquals(1, dao.getFeeds(groupKey = "recommended", limit = 10).size)
        } finally {
            db.close()
        }
    }

    private fun openAtCurrentVersion(): EpisodiveDatabase =
        Room.databaseBuilder(context, EpisodiveDatabase::class.java, TEST_DB)
            // 현재 버전까지 이어서 돈다. 이 테스트가 보는 것은 11→12 의 결과다.
            .addMigrations(Migration11to12, Migration12to13, Migration13to14)
            .allowMainThreadQueries()
            .build()

    private fun createV11DatabaseWithOneFeedAndOneSoundbite() {
        createDatabaseAt(context, TEST_DB, version = 11) { db ->
            db.execSQL(
                "INSERT INTO `feeds` (`id`, `url`, `title`, `newestItemPublishTime`, " +
                    "`description`, `image`, `itunesId`, `language`, `categories`, `groupKey`, " +
                    "`cachedAt`) VALUES (11, 'https://example.com/11', 'Feed 11', 2000, " +
                    "NULL, NULL, NULL, 'en', '', 'trending', 1000)"
            )
            db.execSQL(
                "INSERT INTO `soundbites` (`enclosureUrl`, `title`, `startTime`, `duration`, " +
                    "`episodeId`, `episodeTitle`, `feedTitle`, `feedUrl`, `feedId`, `cachedAt`) " +
                    "VALUES ('https://example.com/clip', 'Clip', 0, 30, 11, 'Episode', " +
                    "'Feed 11', 'https://example.com/11', 11, 1000)"
            )
        }
    }

    companion object {
        private const val TEST_DB = "migration-test.db"
    }
}
