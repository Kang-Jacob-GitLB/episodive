package io.jacob.episodive.core.network.datasource

import com.google.gson.Gson
import io.jacob.episodive.core.network.model.BroadcasterSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class BroadcasterStreamDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var dataSource: BroadcasterStreamDataSource

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val base = server.url("/")
        dataSource = BroadcasterStreamDataSourceImpl(
            callFactory = OkHttpClient(),
            gson = Gson(),
            ioDispatcher = Dispatchers.Unconfined,
            endpoints = BroadcasterEndpoints(kbs = base, mbc = base, sbs = base),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `Given KBS json, When resolve, Then first channel_item service_url`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"channel_item":[{"service_url":"https://kbs.example/live.m3u8?sig=1"},{"service_url":"https://x"}]}"""
            )
        )

        val url = dataSource.resolve(BroadcasterSource.Kbs("21"))

        assertEquals("https://kbs.example/live.m3u8?sig=1", url)
        assertEquals("/api/v1/landing/live/channel_code/21", server.takeRequest().path)
    }

    @Test
    fun `Given KBS json without items, When resolve, Then null`() = runTest {
        server.enqueue(MockResponse().setBody("""{"channel_item":[]}"""))

        assertNull(dataSource.resolve(BroadcasterSource.Kbs("21")))
    }

    @Test
    fun `Given MBC body with whitespace, When resolve, Then trimmed url and query`() = runTest {
        server.enqueue(MockResponse().setBody("https://mbc.example/live.m3u8?t=1\r\n"))

        val url = dataSource.resolve(BroadcasterSource.Mbc("sfm"))

        assertEquals("https://mbc.example/live.m3u8?t=1", url)
        assertEquals("/aacplay.ashx?agent=webapp&channel=sfm", server.takeRequest().path)
    }

    @Test
    fun `Given SBS body, When resolve, Then trimmed url and path with query`() = runTest {
        server.enqueue(MockResponse().setBody("  https://sbs.example/live.m3u8\n"))

        val url = dataSource.resolve(BroadcasterSource.Sbs("powerpc/powerfm"))

        assertEquals("https://sbs.example/live.m3u8", url)
        assertEquals(
            "/play-api/1.0/livestream/powerpc/powerfm?protocol=hls&ssl=Y",
            server.takeRequest().path,
        )
    }

    @Test
    fun `Given non https body, When resolve, Then null`() = runTest {
        server.enqueue(MockResponse().setBody("<html>error</html>"))
        server.enqueue(MockResponse().setBody("http://insecure.example/live.m3u8"))

        assertNull(dataSource.resolve(BroadcasterSource.Mbc("sfm")))
        assertNull(dataSource.resolve(BroadcasterSource.Sbs("lovepc/lovefm")))
    }

    @Test
    fun `Given 5xx, When resolve, Then throws IOException`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))

        val error = runCatching { dataSource.resolve(BroadcasterSource.Mbc("sfm")) }.exceptionOrNull()

        assertTrue(error is IOException)
    }

    @Test
    fun `Given malformed KBS json, When resolve, Then throws`() = runTest {
        server.enqueue(MockResponse().setBody("not json"))

        val error = runCatching { dataSource.resolve(BroadcasterSource.Kbs("21")) }.exceptionOrNull()

        assertNotNull(error)
    }

    @Test
    fun `When resolve, Then no Podcast Index auth headers are sent`() = runTest {
        server.enqueue(MockResponse().setBody("https://mbc.example/live.m3u8"))

        dataSource.resolve(BroadcasterSource.Mbc("sfm"))

        val headers = server.takeRequest().headers
        assertNull(headers["X-Auth-Key"])
        assertNull(headers["X-Auth-Date"])
        assertNull(headers["Authorization"])
    }
}
