package io.jacob.episodive.core.database.datasource

import io.jacob.episodive.core.database.dao.FeedDao
import io.jacob.episodive.core.database.model.FeedEntity
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import kotlin.time.Instant

class FeedLocalDataSourceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = mockk<FeedDao>(relaxed = true)

    private val dataSource: FeedLocalDataSource = FeedLocalDataSourceImpl(
        feedDao = dao,
    )

    private val testFeeds = listOf(
        FeedEntity(
            id = 1L,
            url = "https://example.com/feed/1",
            title = "Test Feed",
            newestItemPublishTime = Instant.fromEpochSeconds(2000L),
            description = "Description",
            image = null,
            itunesId = null,
            language = "en",
            categories = listOf(Category.TECHNOLOGY),
            groupKey = "trending",
            cachedAt = Instant.fromEpochSeconds(1000L),
        )
    )

    @After
    fun teardown() {
        confirmVerified(dao)
    }

    @Test
    fun `Given feeds, When upsertFeeds is called, Then dao upsertFeeds is called`() =
        runTest {
            dataSource.upsertFeeds(testFeeds)

            coVerify { dao.upsertFeeds(testFeeds) }
        }

    @Test
    fun `Given groupKey, When deleteFeedsByGroupKey is called, Then dao deleteFeedsByGroupKey is called`() =
        runTest {
            dataSource.deleteFeedsByGroupKey("trending")

            coVerify { dao.deleteFeedsByGroupKey("trending") }
        }

    @Test
    fun `When deleteFeeds is called, Then dao deleteFeeds is called`() =
        runTest {
            dataSource.deleteFeeds()

            coVerify { dao.deleteFeeds() }
        }

    @Test
    fun `Given feeds and groupKey, When replaceFeedsByGroupKey is called, Then dao replaceFeedsByGroupKey is called`() =
        runTest {
            dataSource.replaceFeedsByGroupKey(testFeeds, "trending")

            coVerify { dao.replaceFeedsByGroupKey(testFeeds, "trending") }
        }

    @Test
    fun `Given groupKey and limit, When getFeeds is called, Then dao getFeeds is called`() =
        runTest {
            dataSource.getFeeds("trending", 10)

            coVerify { dao.getFeeds("trending", 10) }
        }

    @Test
    fun `Given groupKey, When getFeedsPaging is called, Then dao getFeedsPaging is called`() =
        runTest {
            dataSource.getFeedsPaging("trending")

            coVerify { dao.getFeedsPaging("trending") }
        }

    @Test
    fun `Given groupKey offset and limit, When getFeedsPagingList is called, Then dao getFeedsPagingList is called`() =
        runTest {
            dataSource.getFeedsPagingList("trending", 0, 10)

            coVerify { dao.getFeedsPagingList("trending", 0, 10) }
        }

    @Test
    fun `Given groupKey, When getFeedsOldestCachedAt is called, Then dao getFeedsOldestCachedAt is called`() =
        runTest {
            dataSource.getFeedsOldestCachedAt("trending")

            coVerify { dao.getFeedsOldestCachedAt("trending") }
        }
}
