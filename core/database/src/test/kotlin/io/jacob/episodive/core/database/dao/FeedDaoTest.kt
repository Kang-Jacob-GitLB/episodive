package io.jacob.episodive.core.database.dao

import io.jacob.episodive.core.database.RoomDatabaseRule
import io.jacob.episodive.core.database.model.FeedEntity
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
class FeedDaoTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val dbRule = RoomDatabaseRule()

    private lateinit var dao: FeedDao

    @Before
    fun setup() {
        dao = dbRule.db.feedDao()
    }

    private fun createFeed(
        id: Long,
        groupKey: String = "trending",
        title: String = "Feed $id",
        cachedAt: Instant = Instant.fromEpochSeconds(1000L),
    ) = FeedEntity(
        id = id,
        url = "https://example.com/feed/$id",
        title = title,
        newestItemPublishTime = Instant.fromEpochSeconds(2000L),
        description = "Description $id",
        image = null,
        itunesId = null,
        language = "en",
        categories = listOf(Category.TECHNOLOGY),
        groupKey = groupKey,
        cachedAt = cachedAt,
    )

    @Test
    fun `Given feeds, When upsertFeeds is called, Then feeds are inserted and retrieved`() =
        runTest {
            val feeds = listOf(createFeed(1L), createFeed(2L), createFeed(3L))

            dao.upsertFeeds(feeds)

            val result = dao.getFeeds("trending", limit = 10)
            assertEquals(3, result.size)
            assertEquals(listOf(1L, 2L, 3L), result.map { it.id })
        }

    @Test
    fun `Given existing feeds, When upsertFeeds is called with same ids, Then feeds are updated`() =
        runTest {
            dao.upsertFeeds(listOf(createFeed(1L, title = "Original")))

            dao.upsertFeeds(listOf(createFeed(1L, title = "Updated")))

            val result = dao.getFeeds("trending", limit = 10)
            assertEquals(1, result.size)
            assertEquals("Updated", result[0].title)
        }

    @Test
    fun `Given feeds in different groups, When deleteFeedsByGroupKey is called, Then only specified group is deleted`() =
        runTest {
            dao.upsertFeeds(listOf(createFeed(1L, groupKey = "trending"), createFeed(2L, groupKey = "trending")))
            dao.upsertFeeds(listOf(createFeed(3L, groupKey = "recent")))

            dao.deleteFeedsByGroupKey("trending")

            val trending = dao.getFeeds("trending", limit = 10)
            val recent = dao.getFeeds("recent", limit = 10)
            assertEquals(0, trending.size)
            assertEquals(1, recent.size)
            assertEquals(3L, recent[0].id)
        }

    @Test
    fun `Given feeds, When deleteFeeds is called, Then all feeds are deleted`() =
        runTest {
            dao.upsertFeeds(listOf(createFeed(1L), createFeed(2L)))

            dao.deleteFeeds()

            val result = dao.getFeeds("trending", limit = 10)
            assertEquals(0, result.size)
        }

    @Test
    fun `Given existing feeds, When replaceFeedsByGroupKey is called, Then old feeds are removed and new inserted`() =
        runTest {
            dao.upsertFeeds(listOf(createFeed(1L), createFeed(2L)))

            val newFeeds = listOf(createFeed(3L), createFeed(4L))
            dao.replaceFeedsByGroupKey(newFeeds, "trending")

            val result = dao.getFeeds("trending", limit = 10)
            assertEquals(2, result.size)
            assertEquals(listOf(3L, 4L), result.map { it.id })
        }

    @Test
    fun `Given feeds, When getFeeds is called with limit, Then correct count is returned`() =
        runTest {
            dao.upsertFeeds(
                listOf(createFeed(1L), createFeed(2L), createFeed(3L), createFeed(4L), createFeed(5L))
            )

            val result = dao.getFeeds("trending", limit = 3)
            assertEquals(3, result.size)
        }

    @Test
    fun `Given no feeds for groupKey, When getFeeds is called, Then empty list is returned`() =
        runTest {
            val result = dao.getFeeds("non_existent", limit = 10)
            assertEquals(0, result.size)
        }

    @Test
    fun `Given feeds, When getFeedsPagingList is called with offset and limit, Then correct slice is returned`() =
        runTest {
            dao.upsertFeeds(
                (1L..5L).map { createFeed(it) }
            )

            val result = dao.getFeedsPagingList("trending", offset = 2, limit = 2)
            assertEquals(2, result.size)
        }

    @Test
    fun `Given feeds with different cachedAt, When getFeedsOldestCachedAt is called, Then oldest timestamp is returned`() =
        runTest {
            dao.upsertFeeds(
                listOf(
                    createFeed(1L, cachedAt = Instant.fromEpochSeconds(300L)),
                    createFeed(2L, cachedAt = Instant.fromEpochSeconds(100L)),
                    createFeed(3L, cachedAt = Instant.fromEpochSeconds(200L)),
                )
            )

            val oldest = dao.getFeedsOldestCachedAt("trending")
            assertEquals(Instant.fromEpochSeconds(100L), oldest)
        }

    @Test
    fun `Given no feeds for groupKey, When getFeedsOldestCachedAt is called, Then null is returned`() =
        runTest {
            val oldest = dao.getFeedsOldestCachedAt("non_existent")
            assertNull(oldest)
        }
}
