package io.jacob.episodive.core.caption.text

/**
 * BCP-47 언어 태그를 ML Kit `TranslateLanguage.fromLanguageTag` 가 기대하는 형태로 다듬는다.
 * 순수 로직이라 JVM 테스트로 검증한다(ML Kit 클래스는 참조하지 않는다).
 *
 * 주 subtag(언어 부분)만 남긴다: 앞뒤 공백 제거 → 소문자화 → `-`/`_` 앞부분만.
 * "en", "en-US", "en-us", "EN_gb", " ko ", "ko-KR" 은 모두 매칭되지만
 * 빈 문자열/공백뿐인 태그는 null.
 */
fun String?.toPrimaryLanguageSubtag(): String? {
    val trimmed = this?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    return trimmed.substringBefore('-').substringBefore('_').lowercase()
}
