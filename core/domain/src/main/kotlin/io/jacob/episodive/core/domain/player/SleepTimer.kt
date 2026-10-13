package io.jacob.episodive.core.domain.player

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 수면 타이머. 화면(ViewModel)이 아니라 앱 수명으로 산다 — 에피소드 플레이어 시트와 라디오
 * 시트가 같은 타이머를 보고, 시트가 닫혀도 만료 시점에 재생을 멈춰야 하기 때문이다.
 */
interface SleepTimer {
    /** 남은 시간. 타이머가 없으면 null. */
    val remainingMs: StateFlow<Long?>

    /** 만료되어 재생을 멈춘 순간. replay 가 없어, 구독 전에 지난 만료는 전달되지 않는다. */
    val expired: Flow<Unit>

    fun start(durationMs: Long)

    /** 지금 재생 중인 에피소드가 끝날 때까지. 에피소드가 아니면(라이브 등) 시작하지 않는다. */
    fun startEndOfEpisode()

    fun cancel()
}
