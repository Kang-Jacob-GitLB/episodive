package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.download.EpisodeDownloader
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SaveEpisodeUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val episodeRepository = mockk<EpisodeRepository>(relaxed = true)
    private val episodeDownloader = mockk<EpisodeDownloader>(relaxed = true)

    private val useCase = SaveEpisodeUseCase(
        episodeRepository = episodeRepository,
        episodeDownloader = episodeDownloader,
    )

    @After
    fun teardown() {
        confirmVerified(episodeRepository, episodeDownloader)
    }

    @Test
    fun `When toggleSaved returns true, Then upserts episode and downloads`() =
        runTest {
            val episode = episodeTestData
            coEvery { episodeRepository.toggleSavedEpisode(any()) } returns true

            val result = useCase(episode)

            assertTrue(result)
            coVerify {
                episodeRepository.upsertEpisode(episode)
                episodeRepository.toggleSavedEpisode(episode)
                episodeDownloader.downloadEpisode(eq(episode), any())
            }
        }

    @Test
    fun `When toggleSaved returns false and filePath exists, Then deletes file`() =
        runTest {
            val episode = episodeTestData.copy(filePath = "saved/path.mp3")
            coEvery { episodeRepository.toggleSavedEpisode(any()) } returns false

            val result = useCase(episode)

            assertFalse(result)
            coVerify {
                episodeRepository.upsertEpisode(episode)
                episodeRepository.toggleSavedEpisode(episode)
                episodeDownloader.deleteDownloadedFile("saved/path.mp3")
            }
        }

    @Test
    fun `When toggleSaved returns false and filePath null, Then does not delete`() =
        runTest {
            val episode = episodeTestData.copy(filePath = null)
            coEvery { episodeRepository.toggleSavedEpisode(any()) } returns false

            val result = useCase(episode)

            assertFalse(result)
            coVerify {
                episodeRepository.upsertEpisode(episode)
                episodeRepository.toggleSavedEpisode(episode)
            }
            coVerify(exactly = 0) { episodeDownloader.deleteDownloadedFile(any()) }
        }
}
