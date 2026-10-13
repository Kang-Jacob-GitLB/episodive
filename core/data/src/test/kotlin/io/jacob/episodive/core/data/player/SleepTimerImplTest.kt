package io.jacob.episodive.core.data.player

import app.cash.turbine.test
import io.jacob.episodive.core.common.TimeProvider
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SleepTimerImplTest {

    private val playerRepository = mockk<PlayerRepository>(relaxed = true)
    private val timeProvider = mockk<TimeProvider>(relaxed = true)

    private val progressFlow = MutableStateFlow(Progress(0.seconds, 0.seconds, 0.seconds))

    private fun TestScope.createTimer(): SleepTimerImpl {
        every { playerRepository.progress } returns progressFlow
        return SleepTimerImpl(
            playerRepository = playerRepository,
            timeProvider = timeProvider,
            scope = backgroundScope,
            mainDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
    }

    @Test
    fun `Given 고정 타이머, When 만료되면, Then 일시정지하고 expired 를 내보낸다`() = runTest {
        every { timeProvider.currentTimeMillis() } returnsMany listOf(0L, 60_001L)
        val timer = createTimer()

        timer.expired.test {
            timer.start(60_000L)
            awaitItem()
        }

        verify { playerRepository.pause() }
    }

    @Test
    fun `Given 고정 타이머, When 만료되면, Then 볼륨이 1f 로 복구되고 남은 시간은 null 이다`() = runTest {
        every { timeProvider.currentTimeMillis() } returnsMany listOf(0L, 60_001L)
        val timer = createTimer()

        timer.start(60_000L)

        verify { playerRepository.setVolume(0f) }
        verify(atLeast = 1) { playerRepository.setVolume(1f) }
        assertNull(timer.remainingMs.value)
    }

    @Test
    fun `Given 실행 중인 타이머, When cancel 하면, Then 볼륨이 1f 로 복구되고 일시정지는 없다`() = runTest {
        every { timeProvider.currentTimeMillis() } returns 0L
        val timer = createTimer()

        timer.start(60_000L)
        assertEquals(60_000L, timer.remainingMs.value)
        timer.cancel()

        verify(atLeast = 1) { playerRepository.setVolume(1f) }
        verify(exactly = 0) { playerRepository.pause() }
        assertNull(timer.remainingMs.value)
    }

    @Test
    fun `Given 타이머가 없을 때, When cancel 하면, Then 크래시 없이 아무 일도 없다`() = runTest {
        val timer = createTimer()

        timer.cancel()

        verify(exactly = 0) { playerRepository.pause() }
    }

    @Test
    fun `Given 타이머 실행 중, When 연속 start 하면, Then 앞 타이머는 취소되고 뒤 것만 만료된다`() = runTest {
        // 앞 타이머는 0ms 로 시작해 delay 에서 대기하다 취소된다. 뒤 타이머가 만료까지 간다.
        every { timeProvider.currentTimeMillis() } returnsMany listOf(
            0L, 1_000L,
            0L, 30_001L,
        )
        val timer = createTimer()

        timer.start(60_000L)
        timer.start(30_000L)

        verify(exactly = 1) { playerRepository.pause() }
    }

    @Test
    fun `Given 두 타이머가 연달아, When 둘 다 즉시 만료되면, Then 각각 일시정지한다`() = runTest {
        every { timeProvider.currentTimeMillis() } returnsMany listOf(
            0L, 60_001L,
            0L, 30_001L,
        )
        val timer = createTimer()

        timer.start(60_000L)
        timer.start(30_000L)

        verify(exactly = 2) { playerRepository.pause() }
    }

    @Test
    fun `Given 에피소드 끝 타이머, When progress 가 다른 에피소드로 넘어가면, Then 일시정지 없이 멈춘다`() = runTest {
        // 판별은 progress.episodeId 로 한다. nowPlaying 은 DB 왕복 탓에 전환 후에도 한동안 이전
        // 에피소드로 남는다. 남은 시간을 0 으로 주는 것이 중요하다 — 만료 조건을 넘기지 않으면
        // 판별을 어떻게 하든 pause() 가 불리지 않아 이 테스트가 아무것도 잡지 못한다.
        val episodeA = episodeTestDataList[0]
        val episodeB = episodeTestDataList[1]
        progressFlow.value = Progress(10.seconds, 10.seconds, 600.seconds, episodeId = episodeA.id)
        val timer = createTimer()

        timer.startEndOfEpisode()
        progressFlow.value = Progress(20.seconds, 20.seconds, 600.seconds, episodeId = episodeA.id)
        verify(exactly = 0) { playerRepository.pause() }

        progressFlow.value = Progress(600.seconds, 600.seconds, 600.seconds, episodeId = episodeB.id)

        verify(exactly = 0) { playerRepository.pause() }
        assertNull(timer.remainingMs.value)
    }

    @Test
    fun `Given 에피소드 끝 타이머, When 남은 시간이 소진되면, Then 일시정지하고 expired 를 내보낸다`() = runTest {
        val episode = episodeTestData
        progressFlow.value = Progress(10.seconds, 10.seconds, 600.seconds, episodeId = episode.id)
        val timer = createTimer()

        timer.expired.test {
            timer.startEndOfEpisode()
            progressFlow.value = Progress(600.seconds, 600.seconds, 600.seconds, episodeId = episode.id)
            awaitItem()
        }

        verify { playerRepository.pause() }
    }

    @Test
    fun `Given 라이브 스트림(길이 0), When 에피소드 끝 타이머를 걸면, Then 타이머가 뜨지 않는다`() = runTest {
        progressFlow.value = Progress(0.seconds, 0.seconds, 0.seconds, episodeId = episodeTestData.id)
        val timer = createTimer()

        timer.startEndOfEpisode()

        verify(exactly = 0) { playerRepository.pause() }
        assertNull(timer.remainingMs.value)
    }

    @Test
    fun `Given 재생이 정지된 뒤 건 타이머, When 만료되면, Then 화면과 무관하게 expired 를 내보낸다`() = runTest {
        // 타이머는 앱 수명이다. 시트가 닫히고 스테이션이 정지돼도 만료 신호는 구독자에게 도달해야 한다.
        every { timeProvider.currentTimeMillis() } returnsMany listOf(0L, 60_001L)
        val timer = createTimer()
        playerRepository.stop()

        timer.expired.test {
            timer.start(60_000L)
            awaitItem()
        }

        assertNull(timer.remainingMs.value)
    }
}
