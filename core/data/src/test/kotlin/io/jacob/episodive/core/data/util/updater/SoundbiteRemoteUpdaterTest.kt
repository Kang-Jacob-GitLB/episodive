package io.jacob.episodive.core.data.util.updater

import androidx.paging.PagingConfig
import app.cash.turbine.test
import io.jacob.episodive.core.data.util.query.FeedQuery
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.model.SoundbiteResponse
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
    fun `Given dependencies, When Soundbite query, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = FeedQuery.Soundbite
            val updater = SoundbiteRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getSoundbites(any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getSoundbitesOldestCachedAt()
            } returns null
            coEvery {
                remoteDataSource.getRecentSoundbites(any())
            } returns listOf(mockk<SoundbiteResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceSoundbites(any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getSoundbites(10)
                localDataSource.getSoundbitesOldestCachedAt()
                remoteDataSource.getRecentSoundbites(1000)
                localDataSource.replaceSoundbites(any())
            }
        }

    @Test
    fun `Given dependencies, When Soundbite query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = FeedQuery.Soundbite
            val updater = SoundbiteRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getSoundbitesPaging()
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getSoundbitesOldestCachedAt()
            } returns null
            coEvery {
                remoteDataSource.getRecentSoundbites(any())
            } returns listOf(mockk<SoundbiteResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceSoundbites(any())
            } just Runs

            // When
            updater.getPagingData(
                pagingConfig = PagingConfig(
                    pageSize = 10,
                    initialLoadSize = 10,
                    prefetchDistance = 5,
                )
            ).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getSoundbitesOldestCachedAt()
                remoteDataSource.getRecentSoundbites(1000)
                localDataSource.replaceSoundbites(any())
                localDataSource.getSoundbitesPaging()
            }
        }
}