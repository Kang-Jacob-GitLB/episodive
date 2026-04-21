package io.jacob.episodive.core.data.widget

import android.content.Context
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WidgetUpdaterTest {
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val context = mockk<Context>(relaxed = true)

    private fun newUpdater(sink: MutableList<WidgetUpdateRequest>): WidgetUpdater {
        val recordingDispatcher = WidgetDispatcher { _, request -> sink.add(request) }
        return WidgetUpdater(context, testDispatcher, recordingDispatcher)
    }

    @Test
    fun `Given 100 rapid emits at 10ms intervals, when debounced, then dispatch called at most twice`() =
        runTest(testDispatcher) {
            val dispatched = mutableListOf<WidgetUpdateRequest>()
            val updater = newUpdater(dispatched)
            advanceUntilIdle()

            repeat(100) {
                updater.notifyNowPlayingChanged()
                advanceTimeBy(10)
            }
            advanceUntilIdle()

            assertTrue(
                "Expected <= 2 dispatches, got ${dispatched.size}",
                dispatched.size <= 2,
            )
        }

    @Test
    fun `Given distinct request types with debounce settle, when emitted, then each dispatches separately`() =
        runTest(testDispatcher) {
            val dispatched = mutableListOf<WidgetUpdateRequest>()
            val updater = newUpdater(dispatched)
            advanceUntilIdle()

            updater.notifyNowPlayingChanged()
            advanceUntilIdle()
            updater.notifyRecentEpisodesChanged()
            advanceUntilIdle()
            updater.notifyAllWidgets()
            advanceUntilIdle()

            assertEquals(3, dispatched.size)
            assertEquals(WidgetUpdateRequest.NowPlayingChanged, dispatched[0])
            assertEquals(WidgetUpdateRequest.RecentEpisodesChanged, dispatched[1])
            assertEquals(WidgetUpdateRequest.All, dispatched[2])
        }

    @Test
    fun `Given repeated same request, when emitted, then distinctUntilChanged collapses to one dispatch`() =
        runTest(testDispatcher) {
            val dispatched = mutableListOf<WidgetUpdateRequest>()
            val updater = newUpdater(dispatched)
            advanceUntilIdle()

            updater.notifyNowPlayingChanged()
            advanceUntilIdle()
            updater.notifyNowPlayingChanged()
            advanceUntilIdle()
            updater.notifyNowPlayingChanged()
            advanceUntilIdle()

            assertEquals(1, dispatched.size)
            assertEquals(WidgetUpdateRequest.NowPlayingChanged, dispatched[0])
        }
}
