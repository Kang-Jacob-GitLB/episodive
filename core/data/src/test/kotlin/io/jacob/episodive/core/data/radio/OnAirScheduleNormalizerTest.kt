package io.jacob.episodive.core.data.radio

import io.jacob.episodive.core.model.OnAirProgram
import io.jacob.episodive.core.testing.model.onAirProgramTestData
import org.junit.Assert.assertEquals
import org.junit.Test

class OnAirScheduleNormalizerTest {
    private val minute = 60_000L
    private val base = 1_800_000_000_000L

    private fun at(min: Int) = base + min * minute

    private fun program(title: String, from: Int, to: Int, host: String? = null) =
        onAirProgramTestData(title, at(from), at(to), host = host)

    @Test
    fun `Given 같은 제목이 이어진 조각, Then 하나로 합쳐 시작은 첫 조각 끝은 마지막 조각이다`() {
        val result = listOf(
            program("키스더라디오", 0, 60, "가"),
            program("키스더라디오", 60, 120, "가"),
        ).normalizeSchedule()

        assertEquals(listOf(program("키스더라디오", 0, 120, "가")), result)
    }

    @Test
    fun `Given 네 조각, Then 하나로 합쳐진다`() {
        val result = listOf(
            program("가요광장", 0, 30, "나"),
            program("가요광장", 30, 60, "나"),
            program("가요광장", 60, 90, "나"),
            program("가요광장", 90, 120, "나"),
        ).normalizeSchedule()

        assertEquals(listOf(program("가요광장", 0, 120, "나")), result)
    }

    @Test
    fun `Given 진행자가 다름, Then 합치지 않는다`() {
        val a = program("프로", 0, 60, "가")
        val b = program("프로", 60, 120, "나")

        assertEquals(listOf(a, b), listOf(a, b).normalizeSchedule())
    }

    @Test
    fun `Given 제목이 다름, Then 합치지 않는다`() {
        val a = program("앞", 0, 60)
        val b = program("뒤", 60, 120)

        assertEquals(listOf(a, b), listOf(a, b).normalizeSchedule())
    }

    @Test
    fun `Given 같은 제목 사이에 실제 공백, Then 합치지 않는다`() {
        val a = program("프로", 0, 60)
        val b = program("프로", 90, 120)

        assertEquals(listOf(a, b), listOf(a, b).normalizeSchedule())
    }

    @Test
    fun `Given 같은 프로그램 사이에 필러, Then 필러는 빠지고 하나로 합쳐진다`() {
        val result = listOf(
            program("가요광장", 0, 59, "나"),
            program("재난재해 예방캠페인", 59, 60),
            program("가요광장", 60, 120, "나"),
        ).normalizeSchedule()

        assertEquals(listOf(program("가요광장", 0, 120, "나")), result)
    }

    @Test
    fun `Given 다른 프로그램 사이에 필러, Then 필러는 빠지고 앞 프로그램이 틈을 이어받는다`() {
        val result = listOf(
            program("앞", 0, 59),
            program("홍보 스팟", 59, 60),
            program("뒤", 60, 120),
        ).normalizeSchedule()

        assertEquals(listOf(program("앞", 0, 60), program("뒤", 60, 120)), result)
    }

    @Test
    fun `Given 필러 시각, Then 앞 프로그램이 그 시각을 덮는다`() {
        val result = listOf(
            program("앞", 0, 59),
            program("홍보 스팟", 59, 60),
            program("뒤", 60, 120),
        ).normalizeSchedule()

        val atFiller = at(59) + 30_000L
        assertEquals("앞", result.single { it.startMillis!! <= atFiller && atFiller < it.endMillis!! }.title)
    }

    @Test
    fun `Given 맨 끝의 필러, Then 앞 프로그램이 이어받는다`() {
        val result = listOf(
            program("앞", 0, 59),
            program("홍보 스팟", 59, 60),
        ).normalizeSchedule()

        assertEquals(listOf(program("앞", 0, 60)), result)
    }

    @Test
    fun `Given 맨 앞의 필러, Then 그냥 빠진다`() {
        val result = listOf(
            program("홍보 스팟", 0, 1),
            program("뒤", 1, 61),
        ).normalizeSchedule()

        assertEquals(listOf(program("뒤", 1, 61)), result)
    }

    @Test
    fun `Given 정확히 5분짜리 뉴스, Then 필러가 아니다`() {
        val news = program("5시 뉴스", 0, 5)
        val next = program("뒤", 5, 65)

        assertEquals(listOf(news, next), listOf(news, next).normalizeSchedule())
    }

    @Test
    fun `Given 섞인 순서, Then 시작순으로 정렬한다`() {
        val a = program("앞", 0, 60)
        val b = program("뒤", 60, 120)

        assertEquals(listOf(a, b), listOf(b, a).normalizeSchedule())
    }

    @Test
    fun `Given 시각을 모르는 항목, Then 그대로 뒤에 둔다`() {
        val untimed = OnAirProgram("시각 없음", null, null, null, null, null)
        val a = program("앞", 0, 60)

        assertEquals(listOf(a, untimed), listOf(untimed, a).normalizeSchedule())
    }

    @Test
    fun `Given 이미지가 첫 조각에 없음, Then 이어지는 조각의 이미지를 쓴다`() {
        val a = program("프로", 0, 60).copy(imageUrl = null)
        val b = program("프로", 60, 120).copy(imageUrl = "https://img/b.jpg")

        assertEquals("https://img/b.jpg", listOf(a, b).normalizeSchedule().single().imageUrl)
    }
}
