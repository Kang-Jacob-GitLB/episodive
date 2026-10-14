package io.jacob.episodive.core.network.datasource

import com.google.gson.Gson
import io.jacob.episodive.core.network.model.OnAirSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class OnAirDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var dataSource: OnAirDataSource

    // 2026-10-14 는 수요일.
    private val now = millis(2026, 10, 14, 10, 0)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val base = server.url("/")
        dataSource = OnAirDataSourceImpl(
            callFactory = OkHttpClient(),
            gson = Gson(),
            ioDispatcher = Dispatchers.Unconfined,
            endpoints = OnAirEndpoints(kbs = base, mbc = base, sbs = base, cbs = base),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun millis(y: Int, mo: Int, d: Int, h: Int, mi: Int): Long =
        LocalDateTime.of(y, mo, d, h, mi).atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli()

    @Test
    fun `Given KBS schedule, When schedule, Then parses fields and 24h overflow into next day`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """[
                  {"program_planned_date":"20261013","schedules":[
                    {"program_planned_date":"20261013","program_planned_start_time":"25000000",
                     "program_planned_end_time":"26000000","program_title":"심야 방송","program_staff":"",
                     "image_w":"http://img.kbs/night.jpg"}
                  ]},
                  {"program_planned_date":"20261014","schedules":[
                    {"program_planned_date":"20261014","program_planned_start_time":"06150000",
                     "program_planned_end_time":"08000000","program_title":" 오늘 아침 ","program_staff":"정현재",
                     "image_w":"https://img.kbs/morning.jpg"}
                  ]}
                ]"""
            )
        )

        val result = dataSource.schedule(OnAirSource.Kbs("21"), now)

        assertEquals(listOf("심야 방송", "오늘 아침"), result.map { it.title })
        assertEquals(millis(2026, 10, 14, 1, 0), result[0].startMillis)
        assertEquals(millis(2026, 10, 14, 2, 0), result[0].endMillis)
        assertNull(result[0].host)
        assertEquals("https://img.kbs/night.jpg", result[0].imageUrl)
        assertEquals("정현재", result[1].host)
        assertEquals(millis(2026, 10, 14, 6, 15), result[1].startMillis)
        val path = server.takeRequest().requestUrl!!
        assertEquals("21", path.queryParameter("channel_code"))
        assertEquals("20261013", path.queryParameter("program_planned_date_from"))
        assertEquals("20261014", path.queryParameter("program_planned_date_to"))
    }

    @Test
    fun `Given MBC schedule, When schedule, Then filters by weekday and upgrades http image`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """[
                  {"Channel":"FM4U","ProgramTitle":"수요 프로","StartTime":"0900","EndTime":"1100","LiveDays":"월수금",
                   "HDPicture":"http://img.imbc/hd.jpg","Picture":"https://img.imbc/p.jpg"},
                  {"Channel":"FM4U","ProgramTitle":"주말 프로","StartTime":"0900","EndTime":"1100","LiveDays":"토일",
                   "HDPicture":"https://img.imbc/weekend.jpg"},
                  {"Channel":"STFM","ProgramTitle":"다른 채널","StartTime":"0900","EndTime":"1100","LiveDays":"수"},
                  {"Channel":"FM4U","ProgramTitle":"화요 심야","StartTime":"2300","EndTime":"0100","LiveDays":"화",
                   "Picture":"ftp://img.imbc/x.jpg","ThumbnailPicture":"https://img.imbc/t.jpg"}
                ]"""
            )
        )

        val result = dataSource.schedule(OnAirSource.Mbc("FM4U"), now)

        assertEquals(listOf("화요 심야", "수요 프로"), result.map { it.title })
        // 어제 시작해 자정을 넘긴 편성은 종료가 오늘로 넘어온다.
        assertEquals(millis(2026, 10, 13, 23, 0), result[0].startMillis)
        assertEquals(millis(2026, 10, 14, 1, 0), result[0].endMillis)
        assertEquals("https://img.imbc/t.jpg", result[0].imageUrl)
        assertEquals("https://img.imbc/hd.jpg", result[1].imageUrl)
        assertFalse(result.any { it.title == "주말 프로" })
    }

    @Test
    fun `Given MBC song rows, When currentSong, Then trims music symbol for the channel`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """[{"Channel":"STFM","SomItem":"♬ 다른 곡 - 가수"},
                   {"Channel":"FM4U","SomItem":"♬ 봄날 - 방탄소년단 "}]"""
            )
        )

        val song = dataSource.currentSong(OnAirSource.Mbc("FM4U"))

        assertEquals("봄날 - 방탄소년단", song)
    }

    @Test
    fun `Given empty song rows, When currentSong, Then null`() = runTest {
        server.enqueue(MockResponse().setBody("[]"))

        assertNull(dataSource.currentSong(OnAirSource.Mbc("FM4U")))
    }

    @Test
    fun `Given SBS onair list, When schedule, Then picks channel and prefers 1280 thumb and handles 24-00`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"list":[
                  {"channelid":"S08","title":"러브FM 프로","starttime":"09:00","endtime":"11:00",
                   "thumbs":{"640":"https://s/l640.jpg"}},
                  {"channelid":"S07","title":"뮤직하이","subtitle":"SBS","starttime":"23:00","endtime":"24:00",
                   "thumbs":{"640":"https://s/640.jpg","1280":"https://s/1280.jpg"},"thumbimg":"https://s/270.jpg"}
                ]}"""
            )
        )

        val power = dataSource.schedule(OnAirSource.Sbs("S07"), now).single()

        assertEquals("뮤직하이", power.title)
        assertNull(power.host)
        assertEquals("https://s/1280.jpg", power.imageUrl)
        assertEquals(millis(2026, 10, 14, 23, 0), power.startMillis)
        assertEquals(millis(2026, 10, 15, 0, 0), power.endMillis)
    }

    @Test
    fun `Given SBS 640 only, When schedule, Then falls back to 640 then thumbimg`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"list":[{"channelid":"S08","title":"러브","starttime":"09:00","endtime":"11:00",
                  "thumbs":{"640":"https://s/640.jpg"},"thumbimg":"https://s/270.jpg"}]}"""
            )
        )
        assertEquals("https://s/640.jpg", dataSource.schedule(OnAirSource.Sbs("S08"), now).single().imageUrl)

        server.enqueue(
            MockResponse().setBody(
                """{"list":[{"channelid":"S08","title":"러브","starttime":"09:00","endtime":"11:00",
                  "thumbimg":"https://s/270.jpg"}]}"""
            )
        )
        assertEquals("https://s/270.jpg", dataSource.schedule(OnAirSource.Sbs("S08"), now).single().imageUrl)
    }

    @Test
    fun `Given CBS schedule, When schedule, Then POSTs and prefixes relative image and uses mc`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"fm":[],"musicfm":[
                  {"name":"음악 프로","mc":"","startTime":"23:00","endTime":"01:00","date":"2026-10-14",
                   "program":{"mc":"김진행","image":{"radioProfil":"/data/p.png","vod":"/data/v.png"}}},
                  {"name":"이미지 없음","mc":"박진행","startTime":"01:00","endTime":"02:00","date":"2026-10-14","program":null}
                ],"joy4u":[]}"""
            )
        )

        val result = dataSource.schedule(OnAirSource.Cbs("musicfm"), now)

        assertEquals(listOf("이미지 없음", "음악 프로"), result.map { it.title })
        val music = result[1]
        assertEquals("김진행", music.host)
        assertEquals("https://www.cbs.co.kr/data/p.png", music.imageUrl)
        assertEquals(millis(2026, 10, 15, 1, 0), music.endMillis)
        assertEquals("박진행", result[0].host)
        assertNull(result[0].imageUrl)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/schedule/radio/ajax", request.path)
    }

    @Test
    fun `Given any source, When schedule, Then no Podcast Index auth headers`() = runTest {
        server.enqueue(MockResponse().setBody("[]"))

        dataSource.schedule(OnAirSource.Kbs("21"), now)

        val request = server.takeRequest()
        assertNull(request.getHeader("Authorization"))
        assertNull(request.getHeader("X-Auth-Key"))
        assertNull(request.getHeader("X-Auth-Date"))
    }

    @Test(expected = java.io.IOException::class)
    fun `Given HTTP 500, When schedule, Then throws`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        dataSource.schedule(OnAirSource.Kbs("21"), now)
    }
}
