package io.jacob.episodive.core.caption.text

import io.jacob.episodive.core.model.caption.CaptionLanguage

/**
 * sherpa-onnx `greedy_search` 가 내는 토큰 목록을 화면에 띄울 한 줄로 합친다.
 *
 * [com.k2fsa.sherpa.onnx.OnlineRecognizerResult.text] 를 쓰지 않는 이유: sherpa 는 그 필드에서
 * 토큰을 그냥 이어붙여 반환해 띄어쓰기가 통째로 사라진다(스모크 테스트 기록:
 * '걔는괜찮은척하려구애쓰는거같았다'). 대신 `tokens` 배열은 단어 시작 토큰에 선행 공백(영어) 또는
 * BPE 경계 마커 '▁'(U+2581, 한국어) 을 달고 온다 — 그 마커로 직접 띄어쓰기를 복원한다.
 */
object CaptionTextNormalizer {

    /** 한국어 BPE 토크나이저가 단어 시작에 붙이는 경계 마커. */
    private const val WordBoundaryMarker = '▁'

    /** 단독 "i" 와 "i'm"/"i'll"/"i'd"/"i've" 의 "i" 만 잡는다. 아포스트로피는 단어 문자가 아니라
     * `\b` 가 그 앞에서도 경계로 서므로, "i'm" 의 "i" 도 이 패턴 하나로 걸린다. */
    private val StandaloneI = Regex("""\bi\b""")

    /** 한국어는 `.`,`,`,`?`,`!` 앞의 공백을 뗀다. */
    private val SpaceBeforeKoreanPunctuation = Regex(""" +([.,?!])""")

    /** 토큰이 새 단어의 시작인지: 선행 공백 또는 '▁' 마커를 달고 있으면 시작이다. */
    fun isWordStart(token: String): Boolean =
        token.startsWith(WordBoundaryMarker) || token.startsWith(' ')

    /**
     * [tokens] 를 이어붙일 때 실제로 공백이 들어갈 위치(=새 단어가 시작하는 인덱스)를 계산한다.
     *
     * sherpa 는 공백/마커만 있는 토큰(`" "`, `"▁"` 단독)을 독립된 조각으로 낼 때가 있다
     * (KO `' 벌','리고',' ','하진'`, EN `...,'D',' ','QUA',...`). 이 조각은 글자가 없어 그대로
     * 버리면(`isWordStart(token)` 만으로 판정하면) "다음 토큰은 새 단어" 라는 정보까지 같이
     * 사라진다 — 정작 그 다음 토큰(`'하진'`, `'QUA'`)은 자기 앞에 공백을 달고 있지 않기 때문이다.
     * 그 결과 "벌리고하진", "squalidquarter" 처럼 단어가 들러붙는다(실측 스모크 토큰).
     * 그래서 빈 조각이 단어 시작이면 `pendingSpace` 로 기억해 다음 비어 있지 않은 토큰에 넘긴다.
     *
     * [normalize] 와 [CaptionLineTracker.findCutIndex] 모두 "이 인덱스에서 단어가 시작하는가" 를
     * 물어야 하므로, 판정을 여기 하나로 모은다 — 각자 `isWordStart(token)` 만 보고 다시 판정하면
     * 이 함수가 고치는 공백 단독 토큰 케이스에서 둘이 다시 어긋난다.
     */
    fun effectiveWordStarts(tokens: List<String>): BooleanArray {
        val starts = BooleanArray(tokens.size)
        var pendingSpace = false
        for ((index, token) in tokens.withIndex()) {
            val wordStart = isWordStart(token)
            val piece = token.trimStart(WordBoundaryMarker, ' ')
            if (piece.isEmpty()) {
                if (wordStart) pendingSpace = true
                continue
            }
            starts[index] = wordStart || pendingSpace
            pendingSpace = false
        }
        return starts
    }

    /**
     * @param isUtteranceStart 이 토큰 목록이 발화(문장)의 맨 앞이면 true. 영어 첫 글자 대문자화에만 쓴다
     *   — 줄 길이 초과로 화면 표시가 여러 줄로 갈려도, 발화 자체가 이어지는 중이면 false 로 넘긴다.
     */
    fun normalize(language: CaptionLanguage, tokens: List<String>, isUtteranceStart: Boolean): String {
        val starts = effectiveWordStarts(tokens)
        val joined = buildString {
            for ((index, token) in tokens.withIndex()) {
                val piece = token.trimStart(WordBoundaryMarker, ' ')
                if (piece.isEmpty()) continue
                if (starts[index] && isNotEmpty()) append(' ')
                append(piece)
            }
        }
        return when (language) {
            CaptionLanguage.KOREAN -> normalizeKorean(joined)
            CaptionLanguage.ENGLISH -> normalizeEnglish(joined, isUtteranceStart)
        }
    }

    private fun normalizeKorean(text: String): String =
        text.replace(SpaceBeforeKoreanPunctuation, "$1").trim()

    private fun normalizeEnglish(text: String, isUtteranceStart: Boolean): String {
        var result = text.lowercase()
        result = StandaloneI.replace(result, "I")
        if (isUtteranceStart && result.isNotEmpty()) {
            result = result.replaceFirstChar { it.uppercase() }
        }
        return result.trim()
    }
}
