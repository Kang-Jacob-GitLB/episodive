package io.jacob.episodive.core.data.radio

import io.jacob.episodive.core.model.OnAirProgram

/** 이보다 짧은 항목은 프로그램이 아니라 필러(캠페인·홍보 스팟)로 본다. */
private const val FILLER_MAX_MILLIS = 5 * 60_000L

/** 필러가 끼어 있을 때 앞뒤 프로그램을 이어 붙이는 최대 간격. 필러 몇 개 분량이다. */
private const val FILLER_GAP_MAX_MILLIS = 15 * 60_000L

/**
 * 방송사 편성표를 화면에 보일 프로그램 단위로 다듬는다. 방송사·채널 공통이다.
 *
 * - 길이 5분 미만 항목(필러)은 목록에서 뺀다. 시각을 모르는 항목은 그대로 둔다.
 * - 같은 제목·같은 진행자가 시간상 이어지면 한 프로그램으로 합친다(시작=첫 조각, 끝=마지막 조각). 방송사는 한 프로그램을
 *   30분·1시간 조각으로 내려 준다. 사이에 필러만 끼어 있어도 이어진 것으로 본다. 진행자가 다르면 합치지 않는다.
 * - 필러가 낀 틈은 앞 프로그램이 이어받는다. 그러면 필러 시각에도 현재 프로그램이 비지 않아 커버·제목이 잠깐 바뀌지
 *   않고, 앞 프로그램의 끝 시각이 필러 시작이 아니라 다음 프로그램의 시작으로 보인다.
 *
 * 결과는 시작 시각순이다. 이 목록이 편성 목록과 현재 프로그램 판정의 단일 출처다 — 둘이 같은 데이터를 보므로 시트의 방송
 * 시간이 편성 행과 어긋나지 않는다.
 */
internal fun List<OnAirProgram>.normalizeSchedule(): List<OnAirProgram> {
    val timed = filter { it.startMillis != null && it.endMillis != null }.sortedBy { it.startMillis }
    val untimed = filter { it.startMillis == null || it.endMillis == null }

    val result = mutableListOf<OnAirProgram>()
    // 마지막 프로그램 뒤로 건너뛴 필러. 다음 프로그램이 오면 틈을 메우는 데 쓴다.
    var skippedFillers = 0
    var fillerEnd = Long.MIN_VALUE

    for (item in timed) {
        val start = item.startMillis!!
        val end = item.endMillis!!
        if (end - start < FILLER_MAX_MILLIS) {
            skippedFillers++
            fillerEnd = maxOf(fillerEnd, end)
            continue
        }

        val last = result.lastOrNull()
        if (last == null) {
            result += item
        } else {
            val gap = start - last.endMillis!!
            val contiguous = gap <= 0L || (skippedFillers > 0 && gap <= FILLER_GAP_MAX_MILLIS)
            result[result.lastIndex] = when {
                contiguous && last.isSameShowAs(item) -> last.copy(
                    endMillis = maxOf(last.endMillis!!, end),
                    imageUrl = last.imageUrl ?: item.imageUrl,
                )
                // 다른 프로그램이면 필러가 낀 틈만 앞 프로그램이 이어받는다. 필러 없는 실제 공백은 그대로 둔다.
                contiguous && gap > 0L -> last.copy(endMillis = start)
                else -> last
            }
            if (!(contiguous && last.isSameShowAs(item))) result += item
        }
        skippedFillers = 0
        fillerEnd = Long.MIN_VALUE
    }

    // 맨 끝의 필러는 앞 프로그램이 이어받는다.
    val tail = result.lastOrNull()
    if (tail != null && skippedFillers > 0 && fillerEnd - tail.endMillis!! in 0..FILLER_GAP_MAX_MILLIS) {
        result[result.lastIndex] = tail.copy(endMillis = fillerEnd)
    }

    return result + untimed
}

private fun OnAirProgram.isSameShowAs(other: OnAirProgram): Boolean =
    title.trim() == other.title.trim() && host?.trim() == other.host?.trim()
