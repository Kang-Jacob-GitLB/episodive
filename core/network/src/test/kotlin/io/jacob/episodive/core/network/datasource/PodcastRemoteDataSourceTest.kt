package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.network.api.PodcastApi
import io.jacob.episodive.core.network.model.PodcastResponse
import io.jacob.episodive.core.network.model.ResponseListWrapper
import io.jacob.episodive.core.network.model.ResponseWrapper
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class PodcastRemoteDataSourceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val podcastApi = mockk<PodcastApi>(relaxed = true)

    private val dataSource: PodcastRemoteDataSource = PodcastRemoteDataSourceImpl(
        podcastApi = podcastApi,
    )

    @Test
    fun `Given dependencies, When searchPodcasts called, Then dao called`() =
        runTest {
            // Given
            val dataList = mockk<ResponseListWrapper<PodcastResponse>>(relaxed = true) {
                every { dataList } returns emptyList()
            }
            coEvery { podcastApi.searchPodcasts(any(), any()) } returns dataList

            // When
            dataSource.searchPodcasts("search")

            // Then
            coVerifySequence {
                podcastApi.searchPodcasts(any(), any())
                dataList.dataList
            }
            confirmVerified(podcastApi)
        }

    @Test
    fun `Given dependencies, When getPodcastByFeedId called, Then dao called`() =
        runTest {
            // Given
            val data = mockk<ResponseWrapper<PodcastResponse>>(relaxed = true) {
                every { data } returns mockk()
            }
            coEvery { podcastApi.getPodcastByFeedId(any()) } returns data

            // When
            dataSource.getPodcastByFeedId(123L)

            // Then
            coVerifySequence {
                podcastApi.getPodcastByFeedId(any())
                data.data
            }
            confirmVerified(podcastApi)
        }

    @Test
    fun `Given dependencies, When getPodcastByFeedUrl called, Then dao called`() =
        runTest {
            // Given
            val data = mockk<ResponseWrapper<PodcastResponse>>(relaxed = true) {
                every { data } returns mockk()
            }
            coEvery { podcastApi.getPodcastByFeedUrl(any()) } returns data

            // When
            dataSource.getPodcastByFeedUrl("https://feed.url")

            // Then
            coVerifySequence {
                podcastApi.getPodcastByFeedUrl(any())
                data.data
            }
            confirmVerified(podcastApi)
        }

    @Test
    fun `Given dependencies, When getPodcastByGuid called, Then dao called`() =
        runTest {
            // Given
            val data = mockk<ResponseWrapper<PodcastResponse>>(relaxed = true) {
                every { data } returns mockk()
            }
            coEvery { podcastApi.getPodcastByGuid(any()) } returns data

            // When
            dataSource.getPodcastByGuid("some-guid")

            // Then
            coVerifySequence {
                podcastApi.getPodcastByGuid(any())
                data.data
            }
            confirmVerified(podcastApi)
        }

    @Test
    fun `Given dependencies, When getPodcastsByMedium called, Then dao called`() =
        runTest {
            // Given
            val dataList = mockk<ResponseListWrapper<PodcastResponse>>(relaxed = true) {
                every { dataList } returns emptyList()
            }
            coEvery { podcastApi.getPodcastsByMedium(any(), any()) } returns dataList

            // When
            dataSource.getPodcastsByMedium("medium")

            // Then
            coVerifySequence {
                podcastApi.getPodcastsByMedium(any(), any())
                dataList.dataList
            }
            confirmVerified(podcastApi)
        }

    @Test
    fun `Given dependencies, When getPodcastsByGuids called, Then dao called`() =
        runTest {
            // Given
            val dataList = mockk<ResponseListWrapper<PodcastResponse>>(relaxed = true) {
                every { dataList } returns emptyList()
            }
            coEvery { podcastApi.getPodcastsByGuids(any()) } returns dataList

            // When
            dataSource.getPodcastsByGuids(listOf("guid1", "guid2"))

            // Then
            coVerifySequence {
                podcastApi.getPodcastsByGuids(any())
                dataList.dataList
            }
            confirmVerified(podcastApi)
        }
}