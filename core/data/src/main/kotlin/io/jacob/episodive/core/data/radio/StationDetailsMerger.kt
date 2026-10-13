package io.jacob.episodive.core.data.radio

import io.jacob.episodive.core.model.StationDetails
import io.jacob.episodive.core.network.model.StationDetailsResponse
import java.net.URI

private const val MAX_TAGS = 5
private val VALID_BITRATE_KBPS = 1..512

/**
 * 후보 레코드들을 스테이션 하나의 상세로 합친다. 필드마다 [priority] 순서대로 첫 유효값을 쓴다.
 *
 * 태그만은 필드 단위가 아니라 레코드 단위다 — 태그가 있는 첫 레코드 하나에서만 가져온다. 여러 레코드의
 * 태그를 합치면 서로 다른 방송(예: 오분류된 후보)의 꼬리표가 한 스테이션에 섞인다.
 *
 * 커뮤니티 입력이라 값을 믿지 않는다: 홈페이지는 http/https 만(`javascript:`·`intent:` 차단), codec 은
 * UNKNOWN 제외, bitrate 는 1..512 만(실측에 613·656 같은 값이 섞여 있다). 전부 비면 null.
 *
 * @param priority 카탈로그의 uuid 우선순위. 여기 없는 uuid 의 레코드는 무시한다.
 */
internal fun mergeStationDetails(
    priority: List<String>,
    responses: List<StationDetailsResponse>,
): StationDetails? {
    val ordered = priority.mapNotNull { uuid -> responses.firstOrNull { it.stationUuid == uuid } }

    val homepage = ordered.firstNotNullOfOrNull { it.homepage.toWebUrlOrNull() }
    val codec = ordered.firstNotNullOfOrNull { response ->
        response.codec?.trim()?.takeIf { it.isNotEmpty() && !it.equals("UNKNOWN", ignoreCase = true) }
    }
    val bitrate = ordered.firstNotNullOfOrNull { response ->
        response.bitrate?.takeIf { it in VALID_BITRATE_KBPS }
    }
    val tags = ordered.map { it.tags.toDisplayTags() }.firstOrNull { it.isNotEmpty() }.orEmpty()

    if (homepage == null && codec == null && bitrate == null && tags.isEmpty()) return null
    return StationDetails(homepage = homepage, tags = tags, codec = codec, bitrateKbps = bitrate)
}

private fun String?.toWebUrlOrNull(): String? {
    val value = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val uri = runCatching { URI(value) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase()
    return value.takeIf { (scheme == "http" || scheme == "https") && !uri.host.isNullOrEmpty() }
}

private fun String?.toDisplayTags(): List<String> {
    if (this == null) return emptyList()
    val seen = mutableSetOf<String>()
    return split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() && seen.add(it.lowercase()) }
        .take(MAX_TAGS)
}
