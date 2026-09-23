package io.jacob.episodive.core.caption.engine

import io.jacob.episodive.core.model.caption.CaptionLine
import io.jacob.episodive.core.model.caption.LiveCaption

/**
 * "화면에 지금 무엇을 보여줄지" 만 결정하는 순수 reducer. [LiveCaptionEngine] 이 세션(에피소드)
 * 마다 하나씩 새로 만들어 쓴다(Hilt 가 관리하는 공유 인스턴스가 아니다) — 그래서 DI 로 주입되지
 * 않는다.
 *
 * 원문·번역 줄을 각각 최근 [maxLines] 개만 유지한다(초과분은 가장 오래된 것부터 버린다). 화면은
 * 커버 영역을 채울 만큼 쌓아 두고 넘치는 윗줄을 위로 밀어 올려 가리므로, 이 수는 "커버를 넘칠
 * 만큼" 이면 되고 그 이상은 어차피 보이지 않는다. 너무 작으면 아직 보이는 맨 윗줄이 애니메이션
 * 없이 사라진다.
 *
 * 번역은 [onTranslation] 의 lineId 가 지금까지 받은 것보다 클 때만 받는다 — 뒤늦게 도착한 옛 줄
 * 번역이 순서를 뒤섞지 않게.
 *
 * @param isTranslating 이 세션이 번역을 하는가. 그대로 [LiveCaption.isTranslating] 으로 실린다.
 */
class CaptionPresenter(
    private val episodeId: Long,
    private val isTranslating: Boolean = false,
    private val maxLines: Int = DefaultMaxLines,
) {
    private val finalizedLines = ArrayDeque<CaptionLine>()
    private var partialLine: CaptionLine? = null
    private val translations = ArrayDeque<CaptionLine>()

    /** 지금까지 본 가장 큰 원문 줄 id. [clear] 때 [translationFloor] 로 옮긴다. */
    private var highestLineId: Long? = null

    /**
     * 이 id 이하의 번역은 받지 않는다. [clear] 가 여기에 "지운 구간의 마지막 줄 id" 를 남긴다 —
     * 번역은 따로 launch 된 코루틴이라 구간이 바뀌어도 취소되지 않으므로, 시크 전에 요청된 번역이
     * 시크 뒤에 도착해 새 구간 밑에 붙는 것을 막는다. 줄 id 는 세션 동안 계속 증가한다.
     */
    private var translationFloor: Long? = null

    /** 지금 화면에 그려야 할 값. 원문도 번역도 없으면 null. */
    fun current(): LiveCaption? {
        val lines = displayedLines()
        if (lines.isEmpty() && translations.isEmpty()) return null
        return LiveCaption(
            episodeId = episodeId,
            lines = lines,
            translations = translations.toList(),
            isTranslating = isTranslating,
        )
    }

    /** 아직 흘러가는 부분이 갱신됐다. 같은 id 면 교체, 새 id 면 그 줄로 넘어간다. */
    fun onPartial(line: CaptionLine): LiveCaption? {
        // 말이 없는 동안(구간 직후·발화 사이) tracker 는 빈 partial 을 준다. 그대로 두면 빈
        // Text 가 한 줄 높이를 차지해, 방금 끝난 문장이 위로 한 칸 밀리고 무음에도 오버레이가 뜬다.
        partialLine = line.takeIf { it.text.isNotBlank() }
        noteLineId(line.id)
        return current()
    }

    /** 줄이 확정됐다 — partial 자리를 비우고 확정 목록에 얹는다(초과분은 오래된 것부터 버림). */
    fun onFinalized(line: CaptionLine): LiveCaption? {
        partialLine = null
        finalizedLines.addBounded(line)
        noteLineId(line.id)
        return current()
    }

    /**
     * 진행 중이던 partial 을 확정 줄로 올리고 그 줄을 돌려준다(없으면 null). 오디오가 오버런으로
     * 끊겨 인식 stream 을 새로 열 때 쓴다 — 그 partial 은 더 이어지지 않지만 화면에서 지울
     * 이유도 없다.
     */
    fun finalizePartial(): CaptionLine? {
        val partial = partialLine ?: return null
        val finalized = partial.copy(isFinal = true)
        onFinalized(finalized)
        return finalized
    }

    /**
     * [lineId] 가 지금까지 받아들인 번역보다 최신이고 지난 구간의 줄이 아닐 때만 번역 줄로 얹는다.
     * 늦게 온 번역은 버린다.
     */
    fun onTranslation(lineId: Long, text: String): LiveCaption? {
        val floor = listOfNotNull(translations.lastOrNull()?.id, translationFloor).maxOrNull()
        if (floor != null && lineId <= floor) return current()
        translations.addBounded(CaptionLine(id = lineId, text = text, isFinal = true))
        return current()
    }

    /** 구간(시크·EOS)이 바뀌어 지난 발화의 흔적을 통째로 지운다. */
    fun clear() {
        finalizedLines.clear()
        partialLine = null
        translations.clear()
        // highestLineId 는 줄어들지 않으므로 이전 floor 이상이다.
        translationFloor = highestLineId
    }

    private fun noteLineId(id: Long) {
        highestLineId = maxOf(highestLineId ?: id, id)
    }

    private fun ArrayDeque<CaptionLine>.addBounded(line: CaptionLine) {
        addLast(line)
        while (size > maxLines) removeFirst()
    }

    private fun displayedLines(): List<CaptionLine> {
        val partial = partialLine ?: return finalizedLines.toList()
        return finalizedLines.toList() + partial
    }

    companion object {
        /**
         * 폰 커버(가로 폭 정사각형)에 `bodyMedium` 이 대략 15~18 visual lines, 태블릿 세로는
         * 30 줄 가까이 들어간다. 줄 하나가 최소 1 visual line 이므로 이만큼이면 큰 화면에서도
         * 커버 전체를 넘친다 — 모자라면 아직 보이는 맨 윗줄이 슬라이드 없이 사라진다.
         */
        const val DefaultMaxLines = 60
    }
}
