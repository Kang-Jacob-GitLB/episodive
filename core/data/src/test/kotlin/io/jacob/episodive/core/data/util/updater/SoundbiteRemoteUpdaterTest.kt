package io.jacob.episodive.core.data.util.updater

import io.jacob.episodive.core.data.util.query.FeedQuery
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.model.SoundbiteEntity
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.model.SoundbiteResponse
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
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class SoundbiteRemoteUpdaterTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val localDataSource = mockk<FeedLocalDataSource>(relaxed = true)
    private val remoteDataSource = mockk<FeedRemoteDataSource>(relaxed = true)

    @After
    fun teardown() {
        confirmVerified(localDataSource, remoteDataSource)
    }

    @Test
    fun `Given Soundbite query, When fetchFromRemote, Then calls getRecentSoundbites`() =
        runTest {
            // Given
            val query = FeedQuery.Soundbite
            val updater = SoundbiteRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val mockResponses = listOf(mockk<SoundbiteResponse>(relaxed = true))
            coEvery {
                remoteDataSource.getRecentSoundbites()
            } returns mockResponses

            // When
            val result = updater.fetchFromRemote()

            // Then
            assertTrue(result.isNotEmpty())
            coVerify { remoteDataSource.getRecentSoundbites() }
        }

    @Test
    fun `Given non-Soundbite query, When fetchFromRemote, Then returns empty list`() =
        runTest {
            // Given
            val query = FeedQuery.RecentNew
            val updater = SoundbiteRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )

            // When
            val result = updater.fetchFromRemote()

            // Then
            assertTrue(result.isEmpty())
        }

    @Test
    fun `Given entities, When saveToLocal, Then calls replaceSoundbites`() =
        runTest {
            // Given
            val query = FeedQuery.Soundbite
            val updater = SoundbiteRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val entities = listOf(
                mockk<SoundbiteEntity>(relaxed = true)
            )

            // When
            updater.saveToLocal(entities)

            // Then
            coVerify { localDataSource.replaceSoundbites(entities) }
        }

    @Test
    fun `Given empty output, When isExpired, Then returns true`() =
        runTest {
            // Given
            val query = FeedQuery.Soundbite
            val updater = SoundbiteRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )

            // When
            val result = updater.isExpired(emptyList())

            // Then
            assertTrue(result)
        }

    @Test
    fun `Given expired output, When isExpired, Then returns true`() =
        runTest {
            // Given
            val query = FeedQuery.Soundbite
            val updater = SoundbiteRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val now = Clock.System.now()
            val expiredTime = now - 10.minutes
            val entities = listOf(
                mockk<SoundbiteEntity>(relaxed = true) {
                    coEvery { cachedAt } returns expiredTime
                }
            )

            // When
            val result = updater.isExpired(entities)

            // Then
            assertTrue(result)
        }

    @Test
    fun `Given valid output, When isExpired, Then returns false`() =
        runTest {
            // Given
            val query = FeedQuery.Soundbite
            val updater = SoundbiteRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val now = Clock.System.now()
            val recentTime = now - 3.minutes
            val entities = listOf(
                mockk<SoundbiteEntity>(relaxed = true) {
                    coEvery { cachedAt } returns recentTime
                }
            )

            // When
            val result = updater.isExpired(entities)

            // Then
            assertFalse(result)
        }

    @Test
    fun `Given expired data, When load, Then fetches from remote and saves to local`() =
        runTest {
            // Given
            val query = FeedQuery.Soundbite
            val updater = SoundbiteRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val mockResponses = listOf(mockk<SoundbiteResponse>(relaxed = true))
            coEvery {
                remoteDataSource.getRecentSoundbites()
            } returns mockResponses
            val now = Clock.System.now()
            val expiredTime = now - 10.minutes
            val entities = listOf(
                mockk<SoundbiteEntity>(relaxed = true) {
                    coEvery { cachedAt } returns expiredTime
                }
            )

            // When
            updater.load(entities)

            // Then
            coVerify { remoteDataSource.getRecentSoundbites() }
            coVerify { localDataSource.replaceSoundbites(any()) }
        }

    @Test
    fun `Given valid data, When load, Then does not fetch from remote`() =
        runTest {
            // Given
            val query = FeedQuery.Soundbite
            val updater = SoundbiteRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val now = Clock.System.now()
            val recentTime = now - 3.minutes
            val entities = listOf(
                mockk<SoundbiteEntity>(relaxed = true) {
                    coEvery { cachedAt } returns recentTime
                }
            )

            // When
            updater.load(entities)

            // Then
            coVerify(exactly = 0) { remoteDataSource.getRecentSoundbites() }
            coVerify(exactly = 0) { localDataSource.replaceSoundbites(any()) }
        }

    @Test
    fun `Given exception during fetch, When load, Then handles exception gracefully`() =
        runTest {
            // Given
            val query = FeedQuery.Soundbite
            val updater = SoundbiteRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            coEvery {
                remoteDataSource.getRecentSoundbites()
            } throws RuntimeException("Network error")

            // When
            updater.load(emptyList())

            // Then - should not throw exception
            coVerify { remoteDataSource.getRecentSoundbites() }
            coVerify(exactly = 0) { localDataSource.replaceSoundbites(any()) }
        }
}