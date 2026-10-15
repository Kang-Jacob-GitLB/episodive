package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.network.api.RadioBrowserApi
import io.jacob.episodive.core.network.model.StationDetailsResponse
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

    @Test
    fun `Given uuids, When getStationDetails called, Then one request with comma joined query`() = runTest {
        // Given
        server.enqueue(MockResponse().setBody(stationDetailsJsonTestData))

        // When
        val details = dataSource.getStationDetails(listOf("a", "b"))

        // Then
        // 쉼표는 전송 중 %2C 로 인코딩되지만 서버가 디코딩한 값은 a,b 다.
        val request = server.takeRequest()
        assertEquals("/json/stations/byuuid", request.requestUrl?.encodedPath)
        assertEquals("a,b", request.requestUrl?.queryParameter("uuids"))
        assertEquals(1, server.requestCount)
        assertEquals(stationDetailsResponseTestData, details.single())
    }

    @Test
    fun `Given empty array, When getStationDetails called, Then returns empty list`() = runTest {
        // Given
        server.enqueue(MockResponse().setBody("[]"))

        // When & Then
        assertEquals(emptyList<StationDetailsResponse>(), dataSource.getStationDetails(listOf("a")))
    }

    @Test
    fun `Given missing fields, When getStationDetails called, Then fields are null`() = runTest {
        // Given
        server.enqueue(MockResponse().setBody("""[{"stationuuid":"a"}]"""))

        // When
        val details = dataSource.getStationDetails(listOf("a")).single()

        // Then
        assertEquals(StationDetailsResponse(stationUuid = "a"), details)
    }

    @Test
    fun `Given country code, When getStationsByCountry called, Then search query is fixed and fields parsed`() = runTest {
        // Given
        server.enqueue(
            MockResponse().setBody(
                """[{"stationuuid":"u1","name":"Radio One","url_resolved":"https://s.example/live","state":"California",""" +
                    """"languagecodes":"en,es","codec":"MP3","hls":0,"lastcheckok":1,"ssl_error":0,"clickcount":99,"is_https":true}]"""
            )
        )

        // When
        val stations = dataSource.getStationsByCountry("US")

        // Then
        val url = server.takeRequest().requestUrl
        assertEquals("/json/stations/search", url?.encodedPath)
        assertEquals("US", url?.queryParameter("countrycode"))
        assertEquals("true", url?.queryParameter("hidebroken"))
        assertEquals("clickcount", url?.queryParameter("order"))
        assertEquals("true", url?.queryParameter("reverse"))
        assertEquals("200", url?.queryParameter("limit"))
        assertEquals(
            StationDetailsResponse(
                stationUuid = "u1",
                name = "Radio One",
                urlResolved = "https://s.example/live",
                state = "California",
                languageCodes = "en,es",
                codec = "MP3",
                hls = 0,
                lastCheckOk = 1,
                sslError = 0,
                clickCount = 99,
            ),
            stations.single(),
        )
    }

    // 응답 테스트 데이터는 이 모듈 안에 둔다 — :core:testing 은 network 모델을 알지 못한다.
    private val stationDetailsJsonTestData = """
        [{"stationuuid":"a","homepage":"https://radio.example.com","tags":"news,talk","codec":"AAC","bitrate":128,"language":"korean","name":"Radio A"}]
    """.trimIndent()

    private val stationDetailsResponseTestData = StationDetailsResponse(
        stationUuid = "a",
        homepage = "https://radio.example.com",
        tags = "news,talk",
        codec = "AAC",
        bitrate = 128,
        language = "korean",
        name = "Radio A",
    )
}
