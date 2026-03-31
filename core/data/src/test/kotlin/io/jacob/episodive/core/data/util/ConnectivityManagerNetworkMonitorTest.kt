package io.jacob.episodive.core.data.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import app.cash.turbine.test
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowNetwork

@RunWith(RobolectricTestRunner::class)
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

    @Test
    fun `Given null ConnectivityManager, When collecting isOnline, Then emits false and closes`() =
        runTest {
            // Given
            val context = mockk<Context>(relaxed = true) {
                every { getSystemService<ConnectivityManager>() } returns null
            }
            val testDispatcher = UnconfinedTestDispatcher(testScheduler)

            // When
            val monitor = ConnectivityManagerNetworkMonitor(
                context = context,
                ioDispatcher = testDispatcher,
            )

            // Then
            monitor.isOnline.test {
                assertEquals(false, awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `Given ConnectivityManager with no active network, When collecting isOnline, Then emits false`() =
        runTest {
            // Given
            val connectivityManager = mockk<ConnectivityManager>(relaxed = true) {
                every { activeNetwork } returns null
            }
            val context = mockk<Context>(relaxed = true) {
                every { getSystemService<ConnectivityManager>() } returns connectivityManager
            }
            val testDispatcher = UnconfinedTestDispatcher(testScheduler)

            // When
            val monitor = ConnectivityManagerNetworkMonitor(
                context = context,
                ioDispatcher = testDispatcher,
            )

            // Then
            monitor.isOnline.test {
                assertEquals(false, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given real ConnectivityManager, When network becomes available, Then emits true via onAvailable callback`() =
        runTest {
            // Given
            val context = ApplicationProvider.getApplicationContext<Context>()
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val shadowCM = Shadows.shadowOf(connectivityManager)
            val testDispatcher = UnconfinedTestDispatcher(testScheduler)

            // Clear default network so isCurrentlyConnected() returns false initially
            shadowCM.setDefaultNetworkActive(false)

            val monitor = ConnectivityManagerNetworkMonitor(
                context = context,
                ioDispatcher = testDispatcher,
            )

            // When & Then
            monitor.isOnline.test {
                // Initial emission from isCurrentlyConnected()
                val initial = awaitItem()

                // Trigger onAvailable callback via shadow
                val callbacks = shadowCM.networkCallbacks
                val network = ShadowNetwork.newInstance(100)
                callbacks.forEach { it.onAvailable(network) }

                // Should emit true after onAvailable
                val afterAvailable = awaitItem()
                assertEquals(true, afterAvailable)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given network available, When network lost, Then emits false via onLost callback`() =
        runTest {
            // Given
            val context = ApplicationProvider.getApplicationContext<Context>()
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val shadowCM = Shadows.shadowOf(connectivityManager)
            val testDispatcher = UnconfinedTestDispatcher(testScheduler)

            val monitor = ConnectivityManagerNetworkMonitor(
                context = context,
                ioDispatcher = testDispatcher,
            )

            // When & Then
            monitor.isOnline.test {
                // Consume initial emission
                awaitItem()

                val network = ShadowNetwork.newInstance(200)
                val callbacks = shadowCM.networkCallbacks

                // First make network available
                callbacks.forEach { it.onAvailable(network) }
                // Consume the true emission
                assertEquals(true, awaitItem())

                // Now lose the network
                callbacks.forEach { it.onLost(network) }
                // Should emit false (no networks remaining)
                assertEquals(false, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given active network with internet, When collecting isOnline, Then isCurrentlyConnected emits true`() =
        runTest {
            // Given - mock ConnectivityManager to return active network with internet capability
            val network = mockk<Network>()
            val capabilities = mockk<NetworkCapabilities> {
                every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
            }
            val connectivityManager = mockk<ConnectivityManager>(relaxed = true) {
                every { activeNetwork } returns network
                every { getNetworkCapabilities(network) } returns capabilities
            }
            val context = mockk<Context>(relaxed = true) {
                every { getSystemService<ConnectivityManager>() } returns connectivityManager
            }
            val testDispatcher = UnconfinedTestDispatcher(testScheduler)

            val monitor = ConnectivityManagerNetworkMonitor(
                context = context,
                ioDispatcher = testDispatcher,
            )

            // When & Then
            monitor.isOnline.test {
                // isCurrentlyConnected() should return true since active network has internet
                val item = awaitItem()
                assertEquals(true, item)
                cancelAndIgnoreRemainingEvents()
            }
        }
}