package io.jacob.episodive.feature.channel

import app.cash.turbine.test
import io.jacob.episodive.core.domain.usecase.channel.GetChannelByIdUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetPodcastsByChannelUseCase
import io.jacob.episodive.core.testing.model.channelTestData
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChannelViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getChannelByIdUseCase = mockk<GetChannelByIdUseCase>(relaxed = true)
    private val getPodcastsByChannelUseCase = mockk<GetPodcastsByChannelUseCase>(relaxed = true)

    private fun createViewModel(id: Long = 1L): ChannelViewModel {
        return ChannelViewModel(
            getChannelByIdUseCase = getChannelByIdUseCase,
            getPodcastsByChannelUseCase = getPodcastsByChannelUseCase,
            id = id,
        )
    }

    @Test
    fun `Given no emissions, When ViewModel is created, Then initial state is Loading`() = runTest {
        every { getChannelByIdUseCase(any()) } returns flowOf()

        val viewModel = createViewModel()

        assertEquals(ChannelState.Loading, viewModel.state.value)
    }

    @Test
    fun `Given valid channel and podcasts, When flows emit, Then state is Success`() = runTest {
        val channel = channelTestData
        val podcasts = podcastTestDataList.take(3)
        every { getChannelByIdUseCase(1L) } returns flowOf(channel)
        every { getPodcastsByChannelUseCase(channel) } returns flowOf(podcasts)

        val viewModel = createViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is ChannelState.Success)
            val success = state as ChannelState.Success
            assertEquals(channel, success.channel)
            assertEquals(podcasts, success.podcasts)
        }
    }

    @Test
    fun `Given null channel, When flow emits, Then state remains Loading because podcasts flow never emits`() =
        runTest {
            every { getChannelByIdUseCase(1L) } returns flowOf(null)

            val viewModel = createViewModel()

            // When channel is null, podcasts becomes emptyFlow(), so combine never fires
            assertEquals(ChannelState.Loading, viewModel.state.value)
        }

    @Test
    fun `Given flow throws exception, When collecting, Then state is Error`() = runTest {
        every { getChannelByIdUseCase(1L) } returns kotlinx.coroutines.flow.flow {
            throw RuntimeException("Network error")
        }

        val viewModel = createViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is ChannelState.Error)
            assertEquals("Network error", (state as ChannelState.Error).message)
        }
    }

    @Test
    fun `Given valid channel with empty podcasts, When flows emit, Then state is Success with empty list`() =
        runTest {
            val channel = channelTestData
            every { getChannelByIdUseCase(1L) } returns flowOf(channel)
            every { getPodcastsByChannelUseCase(channel) } returns flowOf(emptyList())

            val viewModel = createViewModel()

            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is ChannelState.Success)
                val success = state as ChannelState.Success
                assertEquals(channel, success.channel)
                assertTrue(success.podcasts.isEmpty())
            }
        }

    @Test
    fun `Given ClickBack action, When sent, Then NavigateBack effect is emitted`() = runTest {
        every { getChannelByIdUseCase(1L) } returns flowOf(channelTestData)
        every { getPodcastsByChannelUseCase(any()) } returns flowOf(emptyList())

        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.sendAction(ChannelAction.ClickBack)
            assertEquals(ChannelEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `Given ClickPodcast action, When sent, Then NavigateToPodcast effect is emitted`() =
        runTest {
            every { getChannelByIdUseCase(1L) } returns flowOf(channelTestData)
            every { getPodcastsByChannelUseCase(any()) } returns flowOf(emptyList())

            val viewModel = createViewModel()

            viewModel.effect.test {
                viewModel.sendAction(ChannelAction.ClickPodcast(42L))
                assertEquals(ChannelEffect.NavigateToPodcast(42L), awaitItem())
            }
        }
}
