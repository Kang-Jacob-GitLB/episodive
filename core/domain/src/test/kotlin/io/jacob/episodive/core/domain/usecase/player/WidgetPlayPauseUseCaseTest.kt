package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPlayPauseUseCaseTest {
    private val playerRepository = mockk<PlayerRepository>(relaxed = true)
    private val restoreLastPlayState = mockk<RestoreLastPlayStateUseCase>()

    private val useCase = WidgetPlayPauseUseCase(
        playerRepository = playerRepository,
        restoreLastPlayState = restoreLastPlayState,
    )

    @Test
    fun `Given loaded player, When invoke, Then toggles without restoring`() = runTest {
        every { playerRepository.hasMediaItem() } returns true

        assertTrue(useCase())

        verify { playerRepository.playOrPause() }
        coVerify(exactly = 0) { restoreLastPlayState() }
    }

    @Test
    fun `Given empty player and restorable state, When invoke, Then restores before toggling`() = runTest {
        every { playerRepository.hasMediaItem() } returns false
        coEvery { restoreLastPlayState() } returns true

        assertTrue(useCase())

        coVerifyOrder {
            restoreLastPlayState()
            playerRepository.playOrPause()
        }
    }

    @Test
    fun `Given empty player and nothing to restore, When invoke, Then returns false without toggling`() = runTest {
        every { playerRepository.hasMediaItem() } returns false
        coEvery { restoreLastPlayState() } returns false

        assertFalse(useCase())

        verify(exactly = 0) { playerRepository.playOrPause() }
    }
}
