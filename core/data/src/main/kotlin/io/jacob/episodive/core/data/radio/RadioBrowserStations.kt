package io.jacob.episodive.core.data.radio

import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.network.model.StationDetailsResponse

/** Radio Browser 스테이션 id 의 접두어. 카탈로그 slug 에는 `:` 가 없어(불변식 테스트) 둘이 겹치지 않는다. */
internal const val RADIO_BROWSER_ID_PREFIX = "rb:"

internal const val MAX_RADIO_BROWSER_STATIONS = 50

private val PLAYABLE_CODECS = setOf("MP3", "AAC", "AAC+", "OGG", "OPUS", "FLAC")

internal fun radioBrowserStationId(uuid: String): String = RADIO_BROWSER_ID_PREFIX + uuid

internal fun String.isRadioBrowserStationId(): Boolean =
    startsWith(RADIO_BROWSER_ID_PREFIX) && length > RADIO_BROWSER_ID_PREFIX.length

internal fun String.radioBrowserUuid(): String = removePrefix(RADIO_BROWSER_ID_PREFIX)

/**
 * 국가별 목록에서 걸러 낸 스테이션 하나. [urlResolved] 는 크롤 시점의 주소라 토큰이 붙어 만료될 수 있으므로
 * 메모리에서만 들고, 재생 직전에 `/json/url` 이 실패했을 때의 대체로만 쓴다. DB 에는 싣지 않는다.
 *
 * @property language 표시에 쓰지는 않지만 메타 표에 남기는 언어 코드(소문자 첫 토큰)
 */
internal data class RemoteStation(
    val station: Station,
    val uuid: String,
    val urlResolved: String,
    val isHls: Boolean,
    val language: String?,
)

/**
 * 국가별 인기 목록 응답에서 재생할 수 있는 것만 골라 [limit] 개로 줄인다. 서버 필터(`hidebroken`)만 믿지
 * 않고 클라이언트가 다시 판정한다 — 서버의 `is_https` 도 쓰지 않고 [StationDetailsResponse.urlResolved] 로 직접 본다.
 *
 * 통과 조건: 마지막 점검이 성공이고 SSL 오류가 없으며, 주소가 https 이고, 코덱이 재생 가능한 것이고(UNKNOWN 은
 * HLS 일 때만), 이름이 비어 있지 않다. 코덱 칸에 `MP3,H.264` 처럼 영상이 섞인 것은 허용 목록에 없어 이때 걸러진다.
 *
 * 인기순(클릭 수)으로 정렬한 뒤 같은 방송(영숫자만 남긴 이름이 같거나 주소가 같음)은 앞의 것만 남긴다.
 */
internal fun List<StationDetailsResponse>.toRemoteStations(
    limit: Int = MAX_RADIO_BROWSER_STATIONS,
): List<RemoteStation> {
    val seenNames = mutableSetOf<String>()
    val seenUrls = mutableSetOf<String>()
    val result = mutableListOf<RemoteStation>()
    // sortedByDescending 은 안정 정렬이라 클릭 수가 같으면 서버 순서를 지킨다.
    for (response in sortedByDescending { it.clickCount ?: 0 }) {
        val remote = response.toRemoteStationOrNull() ?: continue
        val nameKey = remote.station.name.filter { it.isLetterOrDigit() }.lowercase()
        val urlKey = remote.urlResolved.lowercase()
        // 이름이 기호뿐이라 키가 비면 이름으로는 중복을 판정하지 않는다.
        if (nameKey.isNotEmpty() && nameKey in seenNames) continue
        if (urlKey in seenUrls) continue
        if (nameKey.isNotEmpty()) seenNames += nameKey
        seenUrls += urlKey
        result += remote
        if (result.size >= limit) break
    }
    return result
}

private fun StationDetailsResponse.toRemoteStationOrNull(): RemoteStation? {
    val uuid = stationUuid?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val name = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (lastCheckOk != 1 || sslError != 0) return null
    val url = urlResolved?.trim()?.takeIf { it.startsWith("https://", ignoreCase = true) && it.length > "https://".length }
        ?: return null

    val isHls = hls == 1 || url.substringBefore('?').endsWith(".m3u8", ignoreCase = true)
    val codec = codec?.trim()?.uppercase().orEmpty()
    val playable = codec in PLAYABLE_CODECS || (codec == "UNKNOWN" && hls == 1)
    if (!playable) return null

    return RemoteStation(
        station = Station(
            id = radioBrowserStationId(uuid),
            name = name,
            broadcaster = "",
            kind = StationKind.RADIO,
            region = state?.trim()?.takeIf { it.isNotEmpty() },
        ),
        uuid = uuid,
        urlResolved = url,
        isHls = isHls,
        language = languageCodes?.substringBefore(',')?.trim()?.lowercase()?.takeIf { it.isNotEmpty() },
    )
}
