package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.model.OnAirProgram
import io.jacob.episodive.core.network.model.OnAirSource

interface OnAirDataSource {
    /**
     * 방송사 편성표에서 [nowMillis] 가 속한 날(Asia/Seoul)의 프로그램 목록을 시작 시각순으로 받는다.
     * KBS 는 전날 자정 넘김 편성을 위해 전날 것도 함께 준다. SBS 는 편성표가 아니라 지금 방송분 하나뿐이다.
     * HTTP 실패·파싱 실패는 예외로 던진다.
     */
    suspend fun schedule(source: OnAirSource, nowMillis: Long): List<OnAirProgram>

    /** MBC 가 지금 틀고 있는 곡("제목 - 가수"). 없으면 null. */
    suspend fun currentSong(source: OnAirSource.Mbc): String?
}
