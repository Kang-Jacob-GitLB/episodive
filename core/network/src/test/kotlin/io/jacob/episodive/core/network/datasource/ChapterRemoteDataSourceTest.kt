package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.network.api.ChapterApi
import io.jacob.episodive.core.network.model.ChaptersResponse
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChapterRemoteDataSourceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val chapterApi = mockk<ChapterApi>(relaxed = true)

    private val dataSource: ChapterRemoteDataSource = ChapterRemoteDataSourceImpl(
        chapterApi = chapterApi,
    )

    @Test
    fun `Given dependencies, when fetchChapters called, then api called`() =
        runTest {
            // Given
            val url = "https://example.com/chapters.json"
            val response = mockk<ChaptersResponse>(relaxed = true)
            coEvery { chapterApi.fetchChapters(any()) } returns response

            // When
            dataSource.fetchChapters(url)

            // Then
            coVerifySequence {
                chapterApi.fetchChapters(url)
            }
            confirmVerified(chapterApi)
        }

    @Test
    fun `Given exception, when fetchChapters called, then returns empty list`() =
        runTest {
            // Given
            val url = "https://example.com/chapters.json"
            coEvery { chapterApi.fetchChapters(any()) } throws Exception("Network error")

            // When
            val result = dataSource.fetchChapters(url)

            // Then
            assertTrue(result.isEmpty())
            coVerifySequence {
                chapterApi.fetchChapters(url)
            }
            confirmVerified(chapterApi)
        }
}