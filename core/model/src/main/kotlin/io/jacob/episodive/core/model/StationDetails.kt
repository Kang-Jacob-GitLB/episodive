package io.jacob.episodive.core.model

/**
 * Radio Browser 가 알려 주는 스테이션 부가 정보. 커뮤니티가 채운 값이라 비어 있거나 틀릴 수 있어,
 * 쓸 만한 것만 정규화해 담는다. 모든 필드가 비어 있으면 객체 자체를 만들지 않는다.
 *
 * @property homepage http/https 주소만.
 * @property tags 최대 5개. 중복 없이 표시용으로 다듬은 것.
 * @property codec 알 수 없음(UNKNOWN)은 null.
 * @property bitrateKbps 비정상 값은 null.
 */
data class StationDetails(
    val homepage: String?,
    val tags: List<String>,
    val codec: String?,
    val bitrateKbps: Int?,
)
