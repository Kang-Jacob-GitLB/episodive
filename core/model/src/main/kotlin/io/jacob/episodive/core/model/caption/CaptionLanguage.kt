package io.jacob.episodive.core.model.caption

/**
 * 라이브 자막(STT)을 지원하는 언어. 값은 BCP-47 주 subtag(소문자).
 */
enum class CaptionLanguage(val value: String) {
    ENGLISH("en"),
    KOREAN("ko"),
}

/**
 * `Episode.feedLanguage` 등 BCP-47 태그 문자열에서 주 subtag 만 뽑아 매칭한다.
 * "en", "en-US", "en-us", "EN_gb", " ko ", "ko-KR" 은 매칭되고,
 * "", null, "fr", "eng" 는 null 이다.
 */
fun String?.toCaptionLanguage(): CaptionLanguage? {
    val subtag = this?.trim()?.lowercase()?.substringBefore('-')?.substringBefore('_')
    if (subtag.isNullOrEmpty()) return null
    return CaptionLanguage.entries.find { it.value == subtag }
}
