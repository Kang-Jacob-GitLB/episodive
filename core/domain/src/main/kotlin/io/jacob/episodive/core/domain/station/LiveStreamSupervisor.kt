package io.jacob.episodive.core.domain.station

import io.jacob.episodive.core.model.Station
import kotlinx.coroutines.flow.Flow

/**
 * 재생 중인 라이브 스테이션이 실패하면 원인에 맞게 복구하고, 못 하면 포기한다.
 * 라이브 스트림은 끝나지 않으므로 끊김·만료는 사용자가 아니라 이 감독자가 이어 붙인다.
 */
interface LiveStreamSupervisor {
    /** 멱등. 메인에서 호출할 것. */
    fun start()

    /** 복구를 포기하고 항목을 비운 스테이션. 구독자가 없을 때의 발행은 버려진다. */
    val unplayable: Flow<Station>
}
