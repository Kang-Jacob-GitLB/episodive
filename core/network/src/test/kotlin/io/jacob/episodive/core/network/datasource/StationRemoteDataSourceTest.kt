package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.network.api.RadioBrowserApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class StationRemoteDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var dataSource: StationRemoteDataSource

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RadioBrowserApi::class.java)
        dataSource = StationRemoteDataSourceImpl(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `Given ok response, When resolveStreamUrl called, Then returns url`() = runTest {
        // Given
        server.enqueue(
            MockResponse().setBody(
                """{"ok":true,"message":"retrieved station url","stationuuid":"u1","name":"n","url":"https://stream.example/live"}"""
            )
        )

        // When
        val url = dataSource.resolveStreamUrl("u1")

        // Then
        assertEquals("https://stream.example/live", url)
        assertEquals("/json/url/u1", server.takeRequest().path)
    }

    @Test
    fun `Given ok false, When resolveStreamUrl called, Then returns null`() = runTest {
        // Given
        server.enqueue(MockResponse().setBody("""{"ok":false,"message":"station not found"}"""))

        // When & Then
        assertNull(dataSource.resolveStreamUrl("u1"))
    }

    @Test
    fun `Given empty url, When resolveStreamUrl called, Then returns null`() = runTest {
        // Given
        server.enqueue(MockResponse().setBody("""{"ok":true,"stationuuid":"u1","url":""}"""))

        // When & Then
        assertNull(dataSource.resolveStreamUrl("u1"))
    }
}
