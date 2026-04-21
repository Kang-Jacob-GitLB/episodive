package io.jacob.episodive.core.data.widget

import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.usecase.episode.GetRecentEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.player.GetNowPlayingUseCase
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class WidgetDataReaderImplTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getNowPlaying = mockk<GetNowPlayingUseCase>(relaxed = true)
    private val getRecentEpisodes = mockk<GetRecentEpisodesUseCase>(relaxed = true)
    private val playerRepository = mockk<PlayerRepository>(relaxed = true)

    private val reader = WidgetDataReaderImpl(
        getNowPlaying = getNowPlaying,
        getRecentEpisodes = getRecentEpisodes,
        playerRepository = playerRepository,
    )

    @Test
    fun `Given no now playing episode, when snapshotNowPlaying called, then returns null`() =
        runTest {
            every { getNowPlaying() } returns flowOf(null)
            every { playerRepository.isPlaying } returns flowOf(false)

            val snapshot = reader.snapshotNowPlaying()

            assertNull(snapshot)
        }

    @Test
    fun `Given an episode and playing state, when snapshotNowPlaying called, then returns mapped snapshot`() =
        runTest {
            every { getNowPlaying() } returns flowOf(episodeTestData)
            every { playerRepository.isPlaying } returns flowOf(true)

            val snapshot = reader.snapshotNowPlaying()!!

            assertEquals(episodeTestData.id, snapshot.episodeId)
            assertEquals(episodeTestData.feedId, snapshot.podcastId)
            assertEquals(episodeTestData.title, snapshot.title)
            assertEquals(episodeTestData.feedTitle, snapshot.feedTitle)
            assertEquals(episodeTestData.image, snapshot.imageUrl)
            assertEquals(true, snapshot.isPlaying)
        }

    @Test
    fun `Given recent episodes flow, when snapshotRecentEpisodes called, then maps provided episodes in order`() =
        runTest {
            val limit = 3
            val sourceEpisodes = episodeTestDataList.take(limit)
            every { getRecentEpisodes(limit) } returns flowOf(sourceEpisodes)

            val snapshots = reader.snapshotRecentEpisodes(limit)

            assertEquals(sourceEpisodes.size, snapshots.size)
            sourceEpisodes.forEachIndexed { index, episode ->
                val snapshot = snapshots[index]
                assertEquals(episode.id, snapshot.id)
                assertEquals(episode.feedId, snapshot.podcastId)
                assertEquals(episode.title, snapshot.title)
                assertEquals(episode.feedTitle, snapshot.feedTitle)
                assertEquals(episode.image, snapshot.imageUrl)
                assertEquals(episode.duration?.inWholeMilliseconds ?: 0L, snapshot.duration)
                assertEquals(episode.datePublished.toEpochMilliseconds(), snapshot.datePublished)
            }
        }
}
