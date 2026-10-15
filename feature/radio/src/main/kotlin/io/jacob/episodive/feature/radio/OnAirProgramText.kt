package io.jacob.episodive.feature.radio

import io.jacob.episodive.core.model.OnAirProgram
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SeoulZone: ZoneId = ZoneId.of("Asia/Seoul")
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** "프로그램 제목 · 진행자". 진행자를 모르면 제목만. */
internal fun OnAirProgram.titleWithHost(): String =
    host?.takeIf { it.isNotBlank() }?.let { "$title · $it" } ?: title

/** "HH:mm – HH:mm". 방송사 편성이 한국 시각이라 기기 시간대와 무관하게 Asia/Seoul 로 그린다. 시각을 모르면 null. */
internal fun OnAirProgram.timeRangeText(): String? {
    val start = startMillis ?: return null
    val end = endMillis ?: return null
    fun Long.format() = TimeFormatter.format(Instant.ofEpochMilli(this).atZone(SeoulZone))
    return "${start.format()} – ${end.format()}"
}

/** 편성표 행의 시작 시각 "HH:mm". 시각을 모르면 빈 문자열. */
internal fun OnAirProgram.startTimeText(): String =
    startMillis?.let { TimeFormatter.format(Instant.ofEpochMilli(it).atZone(SeoulZone)) }.orEmpty()

/** 현재 방송 항목과 같은 프로그램인가. 곡만 다른 사본(`song`)은 같은 것으로 본다. */
internal fun OnAirProgram.isSameSlotAs(other: OnAirProgram?): Boolean =
    other != null && startMillis == other.startMillis && endMillis == other.endMillis && title == other.title
