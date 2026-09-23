package io.jacob.episodive.core.caption.engine

import io.jacob.episodive.core.model.caption.LiveCaption

/**
 * "화면에 지금 무엇을 보여줄지" 만 결정하는 순수 reducer. [LiveCaptionEngine] 이 세션마다 하나씩
 * 새로 만들어 쓴다(Hilt 가 관리하는 공유 인스턴스가 아니다) — 그래서 DI 로 주입되지 않고 시계만
 * 생성자로 받는다.
 *
 * 확정 줄이 뜨자마자 다음 partial 로 바로 덮이면 눈으로 읽기 전에 사라진다. 그래서 확정 줄은
 * 최소 [holdMillis] 동안 그 자리를 지키고, 그 사이 들어오는 partial 은 보류했다가 유지 시간이
 * 지나야 화면에 반영한다.
 */
class CaptionPresenter(
    private val clock: CaptionClock,
    private val holdMillis: Long = DefaultHoldMillis,
) {
    private var displayed: LiveCaption? = null
    private var displayedAt = 0L
    private var pendingPartial: LiveCaption? = null

    /** 지금 화면에 그려야 할 값. */
    fun current(): LiveCaption? = displayed

    private fun isHolding(now: Long): Boolean =
        displayed?.isFinal == true && now - displayedAt < holdMillis

    /** 줄 길이 초과 또는 endpoint 로 새 확정 줄이 나왔다. 즉시 화면을 바꾸고 유지 타이머를 새로 건다. */
    fun onFinalized(line: LiveCaption): LiveCaption? {
        displayed = line
        displayedAt = clock.millis()
        pendingPartial = null
        return displayed
    }

    /**
     * 아직 흘러가는 부분이 갱신됐다. 직전 확정 줄을 유지하는 중이면 화면은 그대로 두고 이
     * 값을 보류해 둔다 — [tick] 이 유지 시간이 지난 뒤 꺼내 쓴다.
     */
    fun onPartial(line: LiveCaption): LiveCaption? {
        val now = clock.millis()
        if (isHolding(now)) {
            pendingPartial = line
            return displayed
        }
        displayed = line
        displayedAt = now
        pendingPartial = null
        return displayed
    }

    /**
     * 새 입력이 없어도(발화 사이 정적 등) 유지 시간이 지났는지는 시간이 흘러야 알 수 있다.
     * [LiveCaptionEngine] 루프가 매 틱 불러, 만료된 보류 partial 을 그제서야 화면에 반영한다.
     */
    fun tick(): LiveCaption? {
        val now = clock.millis()
        val pending = pendingPartial
        if (!isHolding(now) && pending != null) {
            displayed = pending
            displayedAt = now
            pendingPartial = null
        }
        return displayed
    }

    /**
     * 번역이 도착했다. [lineId] 가 **지금 화면에 떠 있는 줄** 과 같을 때만 붙인다 — 그 사이 새
     * 줄로 넘어갔으면 늦게 온 번역은 버린다.
     */
    fun onTranslation(lineId: Long, translation: String): LiveCaption? {
        val current = displayed ?: return null
        if (current.lineId != lineId) return null
        displayed = current.copy(translation = translation)
        return displayed
    }

    /** 구간(시크·EOS)이 바뀌어 지난 발화의 흔적을 통째로 지운다. */
    fun clear() {
        displayed = null
        displayedAt = 0L
        pendingPartial = null
    }

    companion object {
        const val DefaultHoldMillis = 1_500L
    }
}
