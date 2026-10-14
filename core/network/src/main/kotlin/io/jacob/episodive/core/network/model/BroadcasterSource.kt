package io.jacob.episodive.core.network.model

/**
 * 방송사 자체 웹 API 로 스트림 주소를 받을 때 필요한 식별자.
 *
 * Radio Browser 가 주는 서명 URL 은 크롤 시점의 토큰이라 이미 만료돼 있다. 방송사 API 는 호출할
 * 때마다 새 서명 URL 을 준다.
 */
sealed interface BroadcasterSource {
    /** KBS. [channelCode] 는 `channel_code/{code}` 경로 값(예: 21). */
    data class Kbs(val channelCode: String) : BroadcasterSource

    /** MBC. [channel] 은 `aacplay.ashx?channel=` 값(예: sfm). */
    data class Mbc(val channel: String) : BroadcasterSource

    /** SBS. [path] 는 `livestream/` 아래 경로(예: powerpc/powerfm). */
    data class Sbs(val path: String) : BroadcasterSource
}
