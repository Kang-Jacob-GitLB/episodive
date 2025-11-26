package io.jacob.episodive.core.data.util

import android.content.Context
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class ConnectivityManagerNetworkMonitorTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `Given dependencies, when creating instance, then NetworkMonitor is created`() {
        // Given
        val context = mockk<Context>(relaxed = true)
        val testDispatcher = UnconfinedTestDispatcher()

        // When
        val networkMonitor: NetworkMonitor = ConnectivityManagerNetworkMonitor(
            context = context,
            ioDispatcher = testDispatcher,
        )

        // Then
        assertNotNull(networkMonitor)
        assertNotNull(networkMonitor.isOnline)
    }
}