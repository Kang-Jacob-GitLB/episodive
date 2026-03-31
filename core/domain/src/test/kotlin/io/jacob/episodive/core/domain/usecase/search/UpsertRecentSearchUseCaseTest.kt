package io.jacob.episodive.core.domain.usecase.search

import io.jacob.episodive.core.domain.repository.RecentSearchRepository
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class UpsertRecentSearchUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val recentSearchRepository = mockk<RecentSearchRepository>(relaxed = true)

    private val useCase = UpsertRecentSearchUseCase(
        recentSearchRepository = recentSearchRepository
    )

    @After
    fun teardown() {
        confirmVerified(recentSearchRepository)
    }

    @Test
    fun `Given dependencies, When invoked, Then repository is called`() =
        runTest {
            // Given
            val query = "test"
            coEvery { recentSearchRepository.upsertRecentSearch(any<String>()) } just Runs

            // When
            useCase(query)

            // Then
            coVerifySequence {
                recentSearchRepository.upsertRecentSearch(query)
            }
        }

    @Test
    fun `Given podcast, When invoked with podcast, Then repository is called with podcast`() =
        runTest {
            // Given
            val podcast = podcastTestData
            coEvery { recentSearchRepository.upsertRecentSearch(any<Podcast>()) } just Runs

            // When
            useCase(podcast)

            // Then
            coVerifySequence {
                recentSearchRepository.upsertRecentSearch(podcast)
            }
        }

    @Test
    fun `Given episode, When invoked with episode, Then repository is called with episode`() =
        runTest {
            // Given
            val episode = episodeTestData
            coEvery { recentSearchRepository.upsertRecentSearch(any<Episode>()) } just Runs

            // When
            useCase(episode)

            // Then
            coVerifySequence {
                recentSearchRepository.upsertRecentSearch(episode)
            }
        }
}