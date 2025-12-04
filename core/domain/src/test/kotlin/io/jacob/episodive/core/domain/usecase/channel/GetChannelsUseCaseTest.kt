package io.jacob.episodive.core.domain.usecase.channel

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.ChannelRepository
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class GetChannelsUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val channelRepository = mockk<ChannelRepository>(relaxed = true)

    private val useCase = GetChannelsUseCase(
        channelRepository = channelRepository,
    )

    @After
    fun teardown() {
        confirmVerified(channelRepository)
    }

    @Test
    fun `Given dependencies, when invoke called, then repository called`() =
        runTest {
            // Given
            coEvery {
                channelRepository.getChannels()
            } returns mockk(relaxed = true)

            // When
            useCase().test {
                awaitComplete()
            }

            // Then
            coVerifySequence {
                channelRepository.getChannels()
            }
        }
}