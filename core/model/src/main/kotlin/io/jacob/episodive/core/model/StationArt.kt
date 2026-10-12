package io.jacob.episodive.core.model

/**
 * 스테이션 로고 대신 그리는 이니셜 아트. 로고는 상표라 싣지 않고, 이름에서 뽑은 짧은 글자와
 * 방송사별 고정 색으로 대신한다. Compose·Canvas·Glance 가 같은 결과를 그리도록 규칙을 여기 둔다.
 *
 * @property initials 한글은 1음절, Latin 은 최대 4자
 * @property colorArgb 흰 글자를 얹는 배경색. 라이트·다크 구분 없이 같다.
 */
data class StationArt(
    val initials: String,
    val colorArgb: Int,
)

val Station.art: StationArt
    get() = StationArt(
        initials = stationInitials(),
        colorArgb = StationArtPalette[stationColorIndex()],
    )

/** 흰 글자와의 대비가 4.9:1 이상인 10색(ARGB). */
private val StationArtPalette = intArrayOf(
    0xFF2B5C8A.toInt(),
    0xFFB23A2E.toInt(),
    0xFF2E7D6B.toInt(),
    0xFF7A3B8F.toInt(),
    0xFF8A6116.toInt(),
    0xFF1F6E8C.toInt(),
    0xFF8A3B55.toInt(),
    0xFF3A7D5B.toInt(),
    0xFF44546A.toInt(),
    0xFF6B4E2E.toInt(),
)

/** 해시가 아니라 색을 못 박는 방송사. 해시만 쓰면 KBS·SBS·CBS 같은 주요 방송사가 같은 색이 된다. */
private val ExplicitColorIndex = mapOf(
    "KBS" to 0,
    "MBC" to 1,
    "SBS" to 2,
    "CBS" to 3,
    "EBS" to 4,
    "YTN" to 5,
    "TBS" to 6,
)

/** 지역 계열사(MBC충북·안동MBC)가 본사와 같은 색을 쓰게 묶는 방송사. */
private val FamilyBroadcasters = listOf("KBS", "MBC", "SBS", "CBS", "EBS")

/** 이름에서 이니셜을 가를 때 건너뛰는 일반어(대문자 비교). */
private val GenericWords = setOf("FM", "AM", "RADIO", "라디오", "방송")

private val GenericTails = listOf("FM", "AM", "라디오", "방송")

private const val NoInitial = "·"

private fun Station.stationColorIndex(): Int {
    val family = FamilyBroadcasters.firstOrNull { broadcaster.contains(it, ignoreCase = true) }
        ?: broadcaster
    return ExplicitColorIndex[family] ?: Math.floorMod(family.hashCode(), StationArtPalette.size)
}

private fun Station.stationInitials(): String {
    // 이름이 방송사명으로 시작하고 더 길면 그 접두어는 모든 방송에 같으니 뗀다.
    val stripped = if (broadcaster.isNotEmpty() &&
        name.length > broadcaster.length &&
        name.startsWith(broadcaster, ignoreCase = true)
    ) {
        name.substring(broadcaster.length)
    } else {
        name
    }

    val tokens = stripped.split(' ', '\t').filter { it.isNotEmpty() && it.uppercase() !in GenericWords }
    // 일반어뿐이면(EBS FM, YTN 라디오) 방송사명 자체가 이니셜의 근거다.
    val base = tokens.firstOrNull() ?: broadcaster.trim().split(' ').firstOrNull { it.isNotEmpty() }.orEmpty()
    return initialsOf(base.removeGenericTail())
}

/** "클래식FM" → "클래식". 앞이 한글 음절일 때만 뗀다("1라디오"·"eFM" 은 그대로). */
private fun String.removeGenericTail(): String {
    for (tail in GenericTails) {
        if (length > tail.length && endsWith(tail, ignoreCase = true) &&
            this[length - tail.length - 1].isHangulSyllable()
        ) {
            return substring(0, length - tail.length)
        }
    }
    return this
}

private fun initialsOf(token: String): String {
    val first = token.firstOrNull() ?: return NoInitial
    return when {
        first.isHangulSyllable() -> first.toString()
        // 숫자는 앞 숫자 최대 2자 + 다음 한 글자(1라디오 → 1라).
        first.isAsciiDigit() -> {
            val digits = token.takeWhile { it.isAsciiDigit() }.take(2)
            digits + token.getOrNull(digits.length)?.toString().orEmpty()
        }

        first.isAsciiLetter() -> {
            val alnum = token.filter { it.isAsciiLetter() || it.isAsciiDigit() }
            if (alnum.length <= 4) alnum else alnum.take(3)
        }

        else -> NoInitial
    }
}

private fun Char.isHangulSyllable() = this in '가'..'힣'

private fun Char.isAsciiDigit() = this in '0'..'9'

private fun Char.isAsciiLetter() = this in 'A'..'Z' || this in 'a'..'z'
