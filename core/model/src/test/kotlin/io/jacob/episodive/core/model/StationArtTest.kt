package io.jacob.episodive.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `:core:testing` 의 팩토리를 쓰지 않고 여기서 직접 만든다. 막는 것은 산출물 형식이다 —
 * `:core:testing` 은 AAR 을 내놓고 이 모듈은 `episodive.jvm.library` 라 그것을 소비할 수 없다
 * ([EpisodeTest] 와 같은 사유).
 */
class StationArtTest {

    private fun station(
        name: String,
        broadcaster: String = "KBS",
        region: String? = null,
    ) = Station(
        id = name,
        name = name,
        broadcaster = broadcaster,
        kind = StationKind.RADIO,
        region = region,
    )

    @Test
    fun `Given 방송사명 접두어, Then 떼고 다음 글자를 쓴다`() {
        assertEquals("해", station("KBS 해피FM").art.initials)
        assertEquals("해", station("kbs 해피FM").art.initials)
    }

    @Test
    fun `Given 한글 뒤 FM 꼬리, Then 꼬리를 떼고 첫 음절을 쓴다`() {
        assertEquals("클", station("KBS 클래식FM").art.initials)
        assertEquals("국", station("국방FM", broadcaster = "국방홍보원").art.initials)
        assertEquals("국", station("국악방송", broadcaster = "국악방송").art.initials)
    }

    @Test
    fun `Given 일반어뿐인 이름, Then 방송사명을 쓴다`() {
        assertEquals("EBS", station("EBS FM", broadcaster = "EBS").art.initials)
        assertEquals("YTN", station("YTN 라디오", broadcaster = "YTN").art.initials)
        assertEquals("TBS", station("TBS FM", broadcaster = "TBS").art.initials)
    }

    @Test
    fun `Given 숫자로 시작, Then 숫자와 다음 한 글자를 쓴다`() {
        assertEquals("1라", station("KBS 1라디오").art.initials)
        assertEquals("10라", station("KBS 10라디오").art.initials)
    }

    @Test
    fun `Given Latin 이름, Then 4자까지는 그대로 넘으면 앞 3자`() {
        assertEquals("FM4U", station("MBC FM4U", broadcaster = "MBC").art.initials)
        assertEquals("eFM", station("TBS eFM", broadcaster = "TBS").art.initials)
        assertEquals("JOY", station("CBS JOY4U", broadcaster = "CBS").art.initials)
    }

    @Test
    fun `Given 기호로 시작, Then 점 하나`() {
        assertEquals("·", station("KBS ★별", broadcaster = "KBS").art.initials)
    }

    @Test
    fun `Given 같은 방송사, Then 색이 같고 호출마다 같다`() {
        assertEquals(station("KBS 해피FM").art.colorArgb, station("KBS 쿨FM").art.colorArgb)
        assertEquals(station("국악방송", "국악방송").art.colorArgb, station("국악방송", "국악방송").art.colorArgb)
    }

    @Test
    fun `Given 지역 계열사, Then 본사와 같은 색을 쓴다`() {
        val mbc = station("MBC 표준FM", broadcaster = "MBC").art.colorArgb
        assertEquals(mbc, station("MBC충북 표준FM", broadcaster = "MBC충북").art.colorArgb)
        assertEquals(mbc, station("안동MBC 표준FM", broadcaster = "안동MBC").art.colorArgb)
    }

    @Test
    fun `Given 해시가 음수인 방송사, Then 예외 없이 불투명한 색이다`() {
        // % 였다면 인덱스가 음수가 되어 던진다.
        val broadcaster = "ABCDEFGHIJK"
        assertTrue(broadcaster.hashCode() < 0)

        val color = station("x", broadcaster = broadcaster).art.colorArgb

        assertEquals(0xFF, color ushr 24)
    }

    @Test
    fun `Given 명시한 일곱 방송사, Then 색이 서로 다르다`() {
        val colors = listOf("KBS", "MBC", "SBS", "CBS", "EBS", "YTN", "TBS")
            .map { station("$it 라디오", broadcaster = it).art.colorArgb }
        assertEquals(7, colors.toSet().size)
        assertNotEquals(colors[0], colors[2])
    }

    @Test
    fun `Given 이니셜 길이, Then 글자 비율이 길수록 작아진다`() {
        val color = 0
        assertEquals(0.44f, StationArt("클", color).textRatio, 0f)
        assertEquals(0.36f, StationArt("1라", color).textRatio, 0f)
        assertEquals(0.30f, StationArt("KBS", color).textRatio, 0f)
        assertEquals(0.25f, StationArt("ABCD", color).textRatio, 0f)
    }
}
