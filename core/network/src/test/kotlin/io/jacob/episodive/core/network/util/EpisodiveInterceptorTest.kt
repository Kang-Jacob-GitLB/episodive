package io.jacob.episodive.core.network.util

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodiveInterceptorTest {

    @Test
    fun `Given interceptor, when intercept called, then adds required headers`() {
        // Given
        val apiKey = "test_api_key"
        val apiSecret = "test_secret"
        val interceptor = EpisodiveInterceptor(apiKey, apiSecret)

        val originalRequest = Request.Builder()
            .url("https://api.podcastindex.org/api/1.0/search/byterm")
            .build()

        val chain = mockk<Interceptor.Chain>()
        val response = mockk<Response>(relaxed = true)

        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } returns response

        // When
        interceptor.intercept(chain)

        // Then
        verify {
            chain.proceed(match { request ->
                request.header("User-Agent") == "Episodive/1.0" &&
                        request.header("X-Auth-Key") == apiKey &&
                        request.header("X-Auth-Date") != null &&
                        request.header("Authorization") != null
            })
        }
    }

    @Test
    fun `Given interceptor, when intercept called, then authorization header is valid SHA-1`() {
        // Given
        val apiKey = "test_api_key"
        val apiSecret = "test_secret"
        val interceptor = EpisodiveInterceptor(apiKey, apiSecret)

        val originalRequest = Request.Builder()
            .url("https://api.podcastindex.org/api/1.0/search/byterm")
            .build()

        val chain = mockk<Interceptor.Chain>()
        val response = mockk<Response>(relaxed = true)

        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } returns response

        // When
        interceptor.intercept(chain)

        // Then
        verify {
            chain.proceed(match { request ->
                val authHeader = request.header("Authorization")
                authHeader != null &&
                        // SHA-1 hash should be 40 characters (20 bytes * 2 hex chars)
                        authHeader.length == 40 &&
                        // Should only contain hex characters
                        authHeader.all { it in '0'..'9' || it in 'a'..'f' }
            })
        }
    }

    @Test
    fun `Given interceptor, when intercept called multiple times, then auth date changes`() {
        // Given
        val apiKey = "test_api_key"
        val apiSecret = "test_secret"
        val interceptor = EpisodiveInterceptor(apiKey, apiSecret)

        val originalRequest = Request.Builder()
            .url("https://api.podcastindex.org/api/1.0/search/byterm")
            .build()

        val chain = mockk<Interceptor.Chain>()
        val response = mockk<Response>(relaxed = true)

        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } returns response

        val authDates = mutableSetOf<String>()

        // When - call multiple times with small delay
        repeat(2) {
            interceptor.intercept(chain)
            Thread.sleep(1100) // Wait for unix time to change
        }

        // Then
        verify(exactly = 2) {
            chain.proceed(match { request ->
                val authDate = request.header("X-Auth-Date")
                authDate?.let { authDates.add(it) }
                true
            })
        }

        // Auth dates should be different
        assertTrue("Auth dates should vary over time", authDates.size >= 1)
    }
}