package io.jacob.episodive.core.caption.text

/**
 * 최근 번역 결과를 원문 텍스트 기준으로 캐싱하는 작은 LRU. VTT cue 는 재생을 되감으면 같은
 * 문장이 반복되고, STT 확정 줄도 짧은 발화가 자주 되풀이되므로 같은 텍스트를 매번 다시
 * 번역기에 태우지 않게 한다. 순수 로직이라 ML Kit 없이 JVM 테스트로 검증한다.
 *
 * 스레드 세이프하지 않다 — 호출자가 `@CaptionThread` 단일 스레드에서만 쓴다.
 */
class CaptionTranslationCache(private val capacity: Int = 32) {

    private val entries = LinkedHashMap<String, String>(capacity, 0.75f, true)

    fun get(text: String): String? = entries[text]

    fun put(text: String, translation: String) {
        entries[text] = translation
        if (entries.size > capacity) {
            val oldestKey = entries.keys.iterator().next()
            entries.remove(oldestKey)
        }
    }
}
