package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.model.OnAirProgram
import io.jacob.episodive.core.network.model.OnAirSongLog
import io.jacob.episodive.core.network.model.OnAirSource

interface OnAirDataSource {
    /**
     * 방송사 편성표에서 [nowMillis] 가 속한 날(Asia/Seoul)의 프로그램 목록을 시작 시각순으로 받는다.
     * 자정 넘김 편성을 위해 KBS·MBC·SBS 는 전날 것도 함께 준다.
     * HTTP 실패·파싱 실패는 예외로 던진다.
     */
    suspend fun schedule(source: OnAirSource, nowMillis: Long): List<OnAirProgram>

    /** MBC 가 마지막으로 기록한 곡. 없으면 null. 낡았는지는 호출한 쪽이 [OnAirSongLog.loggedAtMillis] 로 가른다. */
    suspend fun currentSong(source: OnAirSource.Mbc): OnAirSongLog?
}
