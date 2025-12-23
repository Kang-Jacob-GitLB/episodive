package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.network.api.FeedApi
import io.jacob.episodive.core.network.model.RecentFeedResponse
import io.jacob.episodive.core.network.model.RecentNewFeedResponse
import io.jacob.episodive.core.network.model.RecentNewValueFeedResponse
import io.jacob.episodive.core.network.model.ResponseListWrapper
import io.jacob.episodive.core.network.model.TrendingFeedResponse
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class FeedRemoteDataSourceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val feedApi = mockk<FeedApi>(relaxed = true)

    private val dataSource: FeedRemoteDataSource = FeedRemoteDataSourceImpl(
        feedApi = feedApi,
    )

    @Test
    fun `Given dependencies, When getTrendingFeeds called, Then dao called`() =
        runTest {
            // Given
            val dataList = mockk<ResponseListWrapper<TrendingFeedResponse>>(relaxed = true) {
                every { dataList } returns emptyList()
            }
            coEvery { feedApi.getTrendingFeeds(any(), any(), any(), any(), any()) } returns dataList

            // When
            dataSource.getTrendingFeeds()

            // Then
            coVerifySequence {
                feedApi.getTrendingFeeds(any(), any(), any(), any(), any())
                dataList.dataList
            }
            confirmVerified(feedApi)
        }

    @Test
    fun `Given dependencies, When getRecentFeeds called, Then dao called`() =
        runTest {
            // Given
            val dataList = mockk<ResponseListWrapper<RecentFeedResponse>>(relaxed = true) {
                every { dataList } returns emptyList()
            }
            coEvery { feedApi.getRecentFeeds(any(), any(), any(), any(), any()) } returns dataList

            // When
            dataSource.getRecentFeeds()

            // Then
            coVerifySequence {
                feedApi.getRecentFeeds(any(), any(), any(), any(), any())
                dataList.dataList
            }
            confirmVerified(feedApi)
        }

    @Test
    fun `Given dependencies, When getRecentNewFeeds called, Then dao called`() =
        runTest {
            // Given
            val dataList = mockk<ResponseListWrapper<RecentNewFeedResponse>>(relaxed = true) {
                every { dataList } returns emptyList()
            }
            coEvery { feedApi.getRecentNewFeeds(any(), any()) } returns dataList

            // When
            dataSource.getRecentNewFeeds()

            // Then
            coVerifySequence {
                feedApi.getRecentNewFeeds(any(), any())
                dataList.dataList
            }
            confirmVerified(feedApi)
        }

    @Test
    fun `Given dependencies, When getRecentNewValueFeeds called, Then dao called`() =
        runTest {
            // Given
            val dataList = mockk<ResponseListWrapper<RecentNewValueFeedResponse>>(relaxed = true) {
                every { dataList } returns emptyList()
            }
            coEvery { feedApi.getRecentNewValueFeeds(any(), any()) } returns dataList

            // When
            dataSource.getRecentNewValueFeeds()

            // Then
            coVerifySequence {
                feedApi.getRecentNewValueFeeds(any(), any())
                dataList.dataList
            }
            confirmVerified(feedApi)
        }
}