package io.jacob.episodive.core.domain.usecase.caption

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.CaptionRepository
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.UserData
import io.jacob.episodive.core.model.caption.CaptionAvailability
import io.jacob.episodive.core.model.caption.CaptionLanguage
import io.jacob.episodive.core.model.caption.CaptionModelState
import io.jacob.episodive.core.model.caption.CaptionSession
import io.jacob.episodive.core.model.caption.CaptionSubject
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class ObserveStationCaptionUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val playerRepository = mockk<PlayerRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val captionRepository = mockk<CaptionRepository>(relaxed = true)

    private val useCase = ObserveStationCaptionUseCase(
        playerRepository = playerRepository,
        userRepository = userRepository,
        captionRepository = captionRepository,
    )

    private val koStation = radioStationTestData.copy(id = "ko-station", language = "ko")
    private val enStation = radioStationTestData.copy(id = "en-station", language = "en")
    private val musicStation = radioStationTestData.copy(id = "music-station", language = null)

    private fun enabled(value: Boolean) {
        every { userRepository.getUserData() } returns flowOf(UserData(isCaptionEnabled = value))
    }

    private fun session(onCancel: () -> Unit = {}) = flow<CaptionSession> {
        try {
            emit(CaptionSession.Running(caption = null))
            awaitCancellation()
        } finally {
            onCancel()
        }
    }

    @Test
    fun `Given 꺼짐, Then 세션 없이 모델 가용성만 보고한다`() = runTest {
        enabled(false)
        every { playerRepository.nowPlayingStation } returns flowOf(koStation)
        every { captionRepository.modelState(CaptionLanguage.KOREAN) } returns flowOf(CaptionModelState.NotInstalled(sizeBytes = 42L))

        useCase().test {
            val state = awaitItem()
            assertFalse(state.isEnabled)
            assertEquals(CaptionAvailability.NotInstalled(CaptionLanguage.KOREAN, 42L), state.availability)
            awaitComplete()
        }
        verify(exactly = 0) { captionRepository.liveCaptions(any(), any()) }
    }

    @Test
    fun `Given 켜짐과 ko 스테이션, Then Station subject 로 ko 세션을 연다`() = runTest {
        enabled(true)
        every { playerRepository.nowPlayingStation } returns flowOf(koStation)
        every { captionRepository.modelState(CaptionLanguage.KOREAN) } returns flowOf(CaptionModelState.Installed)
        every { captionRepository.liveCaptions(CaptionSubject.Station("ko-station"), CaptionLanguage.KOREAN) } returns session()

        useCase().test {
            val state = awaitItem()
            assertEquals(CaptionAvailability.Ready(CaptionLanguage.KOREAN), state.availability)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given 켜짐과 en 스테이션, Then en 세션을 연다`() = runTest {
        enabled(true)
        every { playerRepository.nowPlayingStation } returns flowOf(enStation)
        every { captionRepository.modelState(CaptionLanguage.ENGLISH) } returns flowOf(CaptionModelState.Installed)
        every { captionRepository.liveCaptions(CaptionSubject.Station("en-station"), CaptionLanguage.ENGLISH) } returns session()

        useCase().test {
            assertEquals(CaptionAvailability.Ready(CaptionLanguage.ENGLISH), awaitItem().availability)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given language 가 null 인 스테이션, Then Unsupported 이고 세션을 열지 않는다`() = runTest {
        enabled(true)
        every { playerRepository.nowPlayingStation } returns flowOf(musicStation)

        useCase().test {
            assertEquals(CaptionAvailability.Unsupported, awaitItem().availability)
            awaitComplete()
        }
        verify(exactly = 0) { captionRepository.liveCaptions(any(), any()) }
    }

    @Test
    fun `Given 스테이션이 없음, Then Unknown 이다`() = runTest {
        enabled(true)
        every { playerRepository.nowPlayingStation } returns flowOf(null)

        useCase().test {
            assertEquals(CaptionAvailability.Unknown, awaitItem().availability)
            awaitComplete()
        }
    }

    @Test
    fun `Given 같은 스테이션이 다시 발행됨, Then 세션을 다시 열지 않는다`() = runTest {
        var opened = 0
        val station = MutableStateFlow<Station?>(koStation)
        enabled(true)
        every { playerRepository.nowPlayingStation } returns station
        every { captionRepository.modelState(CaptionLanguage.KOREAN) } returns flowOf(CaptionModelState.Installed)
        every { captionRepository.liveCaptions(any(), any()) } answers {
            opened++
            session()
        }

        useCase().test {
            awaitItem()
            // 감독자 재시작처럼 같은 id·language 의 다른 인스턴스가 다시 온다.
            station.value = koStation.copy(name = "다른 이름")
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, opened)
    }

    @Test
    fun `Given 다른 스테이션으로 바뀜, Then 앞 세션을 취소하고 새 세션을 연다`() = runTest {
        var firstCancelled = false
        val station = MutableStateFlow<Station?>(koStation)
        enabled(true)
        every { playerRepository.nowPlayingStation } returns station
        every { captionRepository.modelState(CaptionLanguage.KOREAN) } returns flowOf(CaptionModelState.Installed)
        every { captionRepository.modelState(CaptionLanguage.ENGLISH) } returns flowOf(CaptionModelState.Installed)
        every { captionRepository.liveCaptions(CaptionSubject.Station("ko-station"), CaptionLanguage.KOREAN) } returns
            session { firstCancelled = true }
        every { captionRepository.liveCaptions(CaptionSubject.Station("en-station"), CaptionLanguage.ENGLISH) } returns session()

        useCase().test {
            assertEquals(CaptionAvailability.Ready(CaptionLanguage.KOREAN), awaitItem().availability)
            station.value = enStation
            assertEquals(CaptionAvailability.Ready(CaptionLanguage.ENGLISH), awaitItem().availability)
            assertEquals(true, firstCancelled)
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 1) { captionRepository.liveCaptions(CaptionSubject.Station("en-station"), CaptionLanguage.ENGLISH) }
    }

    @Test
    fun `Given 사용 중, Then progress 와 nowPlaying 을 구독하지 않는다`() = runTest {
        enabled(false)
        every { playerRepository.nowPlayingStation } returns flowOf(koStation)
        every { captionRepository.modelState(CaptionLanguage.KOREAN) } returns flowOf(CaptionModelState.Installed)

        useCase().test {
            awaitItem()
            awaitComplete()
        }
        verify(exactly = 0) { playerRepository.progress }
        verify(exactly = 0) { playerRepository.nowPlaying }
    }
}
