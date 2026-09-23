package io.jacob.episodive.feature.player

import io.jacob.episodive.core.model.caption.CaptionLine

/**
 * 롤링 영역에 그릴 문단 하나 — 같은 발화에서 나온 연속한 줄을 이어 붙인 것.
 *
 * @param utteranceId 문단의 키. 롤링의 앵커도 이 값으로 잡는다.
 */
internal data class CaptionParagraph(val utteranceId: Long, val text: String)

/**
 * 연속한 같은 [CaptionLine.utteranceId] 줄을 한 문단으로 묶는다(오래된 → 최신 순서 유지).
 *
 * 인식기는 긴 발화를 글자 수 상한에서 끊어 여러 줄로 내보내는데, 그 줄을 각각 블록으로 그리면
 * 줄바꿈이 영역 폭이 아니라 글자 수에서 일어나 조각마다 한두 단어짜리 짧은 줄이 남는다. 문단으로
 * 이으면 줄바꿈은 영역 폭과 발화 경계에서만 생긴다. 인식기는 단어 경계에서만 끊으므로 공백 하나로
 * 잇는다 — 다만 이음 양쪽이 띄어쓰기를 쓰지 않는 문자(한자·가나·태국어 등)면 붙여 잇는다. 번역
 * 영역은 기기 언어로 번역한 조각이라 일본어·중국어일 수 있다. 빈 줄은 건너뛴다.
 */
internal fun captionParagraphs(lines: List<CaptionLine>): List<CaptionParagraph> {
    val paragraphs = mutableListOf<CaptionParagraph>()
    for (line in lines) {
        if (line.text.isBlank()) continue
        val last = paragraphs.lastOrNull()
        if (last != null && last.utteranceId == line.utteranceId) {
            val separator = if (isUnspacedScript(last.text.last()) && isUnspacedScript(line.text.first())) "" else " "
            paragraphs[paragraphs.lastIndex] = last.copy(text = last.text + separator + line.text)
        } else {
            paragraphs += CaptionParagraph(line.utteranceId, line.text)
        }
    }
    return paragraphs
}

/** 단어 사이를 띄우지 않는 문자 체계. 한글은 띄어쓰기를 쓰므로 들지 않는다. */
private val UnspacedScripts = setOf(
    Character.UnicodeScript.HAN,
    Character.UnicodeScript.HIRAGANA,
    Character.UnicodeScript.KATAKANA,
    Character.UnicodeScript.THAI,
    Character.UnicodeScript.LAO,
    Character.UnicodeScript.KHMER,
    Character.UnicodeScript.MYANMAR,
)

private fun isUnspacedScript(char: Char): Boolean =
    Character.UnicodeScript.of(char.code) in UnspacedScripts
