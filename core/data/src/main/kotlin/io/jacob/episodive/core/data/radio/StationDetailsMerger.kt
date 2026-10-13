package io.jacob.episodive.core.data.radio

import io.jacob.episodive.core.model.StationDetails
import io.jacob.episodive.core.network.model.StationDetailsResponse
import java.net.URL

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

/**
 * http/https 주소만 통과시키고 스킴은 소문자로 바로잡는다. `ACTION_VIEW` 는 스킴을 대소문자 구분해 맞추므로
 * "HTTP://x" 를 그대로 넘기면 열 앱이 없어 아무 일도 일어나지 않는다.
 *
 * 파싱은 `java.net.URL` 로 한다. `URI` 는 공백·한글 경로를 거부해 멀쩡한 주소를 버린다. `URL` 은 모르는
 * 스킴(`javascript:`·`intent:`)을 프로토콜 단계에서 던지므로 차단은 그대로다. 공백만 `%20` 으로 바꾼다.
 */
private fun String?.toWebUrlOrNull(): String? {
    val value = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val url = runCatching { URL(value) }.getOrNull() ?: return null
    val scheme = url.protocol.lowercase()
    if ((scheme != "http" && scheme != "https") || url.host.isNullOrEmpty()) return null
    return scheme + value.substring(value.indexOf(':')).replace(" ", "%20")
}

private fun String?.toDisplayTags(): List<String> {
    if (this == null) return emptyList()
    val seen = mutableSetOf<String>()
    return split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() && seen.add(it.lowercase()) }
        .take(MAX_TAGS)
}
