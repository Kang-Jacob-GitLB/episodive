package io.jacob.episodive.core.caption.text

import io.jacob.episodive.core.model.caption.CaptionLanguage
import io.jacob.episodive.core.model.caption.LiveCaption

/**
 * 발화(끝점 사이) 하나 안에서 흘러가는 토큰을 화면 줄로 쪼갠다.
 *
 * sherpa-onnx `greedy_search` 는 이미 내놓은 토큰을 나중에 고치지 않는다(빔 서치와 달리
 * 재점수 매김이 없다) — 그래서 "이미 확정한 앞부분은 다시 보지 않는다" 는 전제가 성립한다.
 * `decodingMethod` 를 `greedy_search` 로 고정한 이유가 이것이다(`SherpaSpeechRecognizerFactory`
 * 참고). 다른 디코딩 방식으로 바꾸면 이 전제가 깨져, 이미 확정 줄로 내보낸 텍스트가 다음
 * 토큰 갱신에서 다르게 나올 수 있다.
 *
 * @param language 정규화에 넘길 언어(대소문자·구두점 규칙이 언어마다 다르다).
 * @param maxLineChars 한 줄에 허용하는 정규화 후 문자 수. [io.jacob.episodive.core.caption.asset.CaptionModelSpec] 레지스트리 값.
 */
class CaptionLineTracker(
    private val language: CaptionLanguage,
    private val maxLineChars: Int,
) {
    /** 이번 발화에서 이미 확정 줄로 내보낸 토큰 수. 이 앞은 다시 정규화하지 않는다. */
    private var committedTokenCount = 0

    /** 화면에 그릴 줄의 식별자. 확정될 때마다(초과 컷·endpoint) 다음 줄로 넘어간다. */
    private var lineId = 0L

    /**
     * [onTokens] 한 번의 결과.
     * @param finalized 이번 호출에서 줄 길이 초과로 새로 확정된 줄. 없으면 null.
     * @param partial 아직 흘러가는 나머지(확정되지 않은 부분). 빈 문자열일 수 있다.
     */
    data class Update(val finalized: LiveCaption?, val partial: LiveCaption)

    /**
     * 디코드 한 번이 끝날 때마다 그 발화의 **처음부터 누적된** 토큰 전체(`stream.tokens()`)를
     * 넘긴다. 남은 부분(아직 확정하지 않은 부분)의 정규화 길이가 [maxLineChars] 를 넘으면,
     * 그 안에 드는 마지막 단어 시작 토큰 앞에서 잘라 확정 줄로 내보낸다 — 단어 중간을 끊지
     * 않기 위해서다. stream 자체는 리셋하지 않는다(발화는 계속 이어진다).
     */
    fun onTokens(episodeId: Long, tokens: List<String>): Update {
        var finalized: LiveCaption? = null

        val remaining = tokens.subList(committedTokenCount.coerceAtMost(tokens.size), tokens.size)
        val remainingText = normalizeRemaining(remaining)
        if (remainingText.length > maxLineChars) {
            val cutIndex = findCutIndex(remaining)
            if (cutIndex > 0) {
                val committedTokens = remaining.subList(0, cutIndex)
                val text = normalize(committedTokens)
                finalized = LiveCaption(episodeId = episodeId, lineId = lineId, text = text, isFinal = true)
                committedTokenCount += cutIndex
                lineId++
            }
        }

        val stillRemaining = tokens.subList(committedTokenCount.coerceAtMost(tokens.size), tokens.size)
        val partial = LiveCaption(
            episodeId = episodeId,
            lineId = lineId,
            text = normalize(stillRemaining),
            isFinal = false,
        )
        return Update(finalized, partial)
    }

    /**
     * 발화가 endpoint 로 끝났다. 남은 전부를 확정 줄로 내보내고 다음 발화를 위해 리셋한다.
     * 남은 것이 없으면(빈 문자열) null.
     */
    fun onEndpoint(episodeId: Long, tokens: List<String>): LiveCaption? {
        val remaining = tokens.subList(committedTokenCount.coerceAtMost(tokens.size), tokens.size)
        val text = normalize(remaining)
        val result = if (text.isEmpty()) {
            null
        } else {
            LiveCaption(episodeId = episodeId, lineId = lineId, text = text, isFinal = true)
        }
        reset()
        return result
    }

    /** 구간(시크·EOS 등)이 바뀌어 이번 발화를 통째로 버릴 때 부른다. */
    fun reset() {
        committedTokenCount = 0
        lineId++
    }

    private fun normalizeRemaining(remaining: List<String>): String = normalize(remaining)

    private fun normalize(tokens: List<String>): String =
        CaptionTextNormalizer.normalize(language, tokens, isUtteranceStart = committedTokenCount == 0)

    /**
     * [remaining] 의 단어 경계 중, 그 앞까지의 정규화 길이가 [maxLineChars] 이내인 **가장 뒤**
     * 경계를 찾는다. 길이는 토큰을 더할수록만 늘어나므로(정규화가 이어붙이기만 한다) 한 번
     * 넘어서면 더 뒤도 늘 넘는다 — 그 지점에서 바로 멈출 수 있다.
     *
     * 첫 단어 하나만으로 이미 [maxLineChars] 를 넘는 경우 자를 곳이 없어 0 을 돌려준다(그 발화는
     * 이번 호출에서 확정하지 않고 다음 토큰을 더 기다린다).
     */
    private fun findCutIndex(remaining: List<String>): Int {
        // 토큰 하나만 보는 isWordStart 대신 effectiveWordStarts 를 쓴다: 공백 단독 토큰이 경계를
        // 다음 토큰으로 넘긴 경우(CaptionTextNormalizer 문서 참고) 그 다음 토큰 자체는 선행 공백이
        // 없어 isWordStart 로는 경계로 잡히지 않는다 — 잡지 못하면 그 자리에서 자르지 않고 넘어가
        // 확정 줄과 다음 줄 사이에 공백이 빠진 채로 이어붙는다.
        val starts = CaptionTextNormalizer.effectiveWordStarts(remaining)
        var cut = 0
        for (index in 1 until remaining.size) {
            if (!starts[index]) continue
            val candidateLength = normalize(remaining.subList(0, index)).length
            if (candidateLength > maxLineChars) break
            cut = index
        }
        return cut
    }
}
