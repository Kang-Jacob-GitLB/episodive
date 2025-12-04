package io.jacob.episodive.core.data.repository

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.ChannelRepository
import io.jacob.episodive.core.network.datasource.ChannelRemoteDataSource
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class ChannelRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val remoteDataSource = mockk<ChannelRemoteDataSource>(relaxed = true)

    private val repository: ChannelRepository = ChannelRepositoryImpl(
        remoteDataSource = remoteDataSource,
    )

    @After
    fun teardown() {
        confirmVerified(
            remoteDataSource,
        )
    }

    @Test
    fun `Given suspend dataSource, When getChannelById is called, Then calls methods of dataSources`() =
        runTest {
            // Given
            coEvery { remoteDataSource.getChannelById(any()) } returns mockk(relaxed = true)

            // When
            repository.getChannelById(1).test {
                awaitItem()
                awaitComplete()
            }

            // Then
            coVerifySequence {
                remoteDataSource.getChannelById(1)
            }
        }

    @Test
    fun `Given suspend dataSource, When getChannels is called, Then calls methods of dataSources`() =
        runTest {
            // Given
            coEvery { remoteDataSource.getChannels() } returns mockk(relaxed = true)

            // When
            repository.getChannels().test {
                awaitItem()
                awaitComplete()
            }

            // Then
            coVerifySequence {
                remoteDataSource.getChannels()
            }
        }
}