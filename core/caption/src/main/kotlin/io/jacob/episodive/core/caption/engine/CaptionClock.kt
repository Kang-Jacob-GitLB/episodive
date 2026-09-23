package io.jacob.episodive.core.caption.engine

/**
 * 지금 시각(밀리초). [CaptionPresenter] 의 확정 줄 유지 시간처럼 실제 시간에 기대는 로직을
 * 테스트가 쥘 수 있도록 밖에서 주입한다. `() -> Long` 이 아니라 인터페이스인 것은
 * `PlaybackSpectrumMonitor.NanoClock` 과 같은 이유다 — `Function0<Long>` 은 부를 때마다
 * `java.lang.Long` 박싱이 생긴다.
 */
fun interface CaptionClock {
    fun millis(): Long
}
