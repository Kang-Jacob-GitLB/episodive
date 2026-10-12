package io.jacob.episodive.core.model

/**
 * 재생 직전에 얻은 스트림.
 *
 * @property isHls 플레이어가 MIME 을 명시하는 근거. 토큰이 붙은 URL 은 확장자로 HLS 여부를
 *   추론하기 불안하다.
 */
data class StationStream(
    val url: String,
    val isHls: Boolean,
)
