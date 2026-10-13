package io.jacob.episodive.core.network.util

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class RadioBrowserInterceptorTest {
    private lateinit var first: MockWebServer
    private lateinit var second: MockWebServer

    @Before
    fun setUp() {
        first = MockWebServer().apply { start() }
        second = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        runCatching { first.shutdown() }
        runCatching { second.shutdown() }
    }

    private fun client() = OkHttpClient.Builder()
        .addInterceptor(RadioBrowserInterceptor(hosts = listOf(first.url("/"), second.url("/"))))
        .build()

    private fun request() = Request.Builder().url("http://placeholder.invalid/json/url/abc").build()

    @Test
    fun `Given interceptor, When request sent, Then no auth headers and Episodive user agent`() {
        // Given
        first.enqueue(MockResponse().setBody("{}"))

        // When
        client().newCall(request()).execute().close()

        // Then
        val recorded = first.takeRequest()
        assertNull(recorded.getHeader("X-Auth-Key"))
        assertNull(recorded.getHeader("X-Auth-Date"))
        assertNull(recorded.getHeader("Authorization"))
        assertTrue(recorded.getHeader("User-Agent")!!.startsWith("Episodive"))
        assertEquals("/json/url/abc", recorded.path)
    }

    @Test
    fun `Given first host down, When request sent, Then retried on next host and remembered`() {
        // Given
        first.shutdown()
        second.enqueue(MockResponse().setBody("{}"))
        second.enqueue(MockResponse().setBody("{}"))
        val client = client()

        // When
        client.newCall(request()).execute().close()
        client.newCall(request()).execute().close()

        // Then
        assertEquals(2, second.requestCount)
    }

    @Test
    fun `Given 4xx response, When request sent, Then does not switch host`() {
        // Given
        first.enqueue(MockResponse().setResponseCode(404))
        first.enqueue(MockResponse().setResponseCode(429))
        val client = client()

        // When
        val code404 = client.newCall(request()).execute().use { it.code }
        val code429 = client.newCall(request()).execute().use { it.code }

        // Then
        assertEquals(404, code404)
        assertEquals(429, code429)
        assertEquals(0, second.requestCount)
    }

    @Test
    fun `Given first host 503, When request sent, Then next host used and remembered`() {
        // Given
        first.enqueue(MockResponse().setResponseCode(503))
        second.enqueue(MockResponse().setBody("{}"))
        second.enqueue(MockResponse().setBody("{}"))
        val client = client()

        // When
        val code1 = client.newCall(request()).execute().use { it.code }
        val code2 = client.newCall(request()).execute().use { it.code }

        // Then
        assertEquals(200, code1)
        assertEquals(200, code2)
        assertEquals(1, first.requestCount)
        assertEquals(2, second.requestCount)
    }

    @Test
    fun `Given all hosts 503, When request sent, Then last 503 returned`() {
        // Given
        first.enqueue(MockResponse().setResponseCode(503))
        second.enqueue(MockResponse().setResponseCode(503))

        // When
        val code = client().newCall(request()).execute().use { it.code }

        // Then
        assertEquals(503, code)
        assertEquals(1, first.requestCount)
        assertEquals(1, second.requestCount)
    }

    @Test
    fun `Given all hosts down, When request sent, Then throws IOException`() {
        // Given
        first.shutdown()
        second.shutdown()

        // When & Then
        assertThrows(IOException::class.java) {
            client().newCall(request()).execute()
        }
    }
}
