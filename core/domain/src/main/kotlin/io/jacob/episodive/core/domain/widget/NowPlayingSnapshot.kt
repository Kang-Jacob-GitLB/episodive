package io.jacob.episodive.core.domain.widget

import io.jacob.episodive.core.model.StationArt

/**
 * 위젯이 그리는 재생 상태. 에피소드면 [episodeId]·[podcastId] 가, 라이브 스테이션이면 [stationId]·
 * [stationArt] 가 채워진다. 스테이션 스냅샷에는 곡 제목(streamTitle)을 싣지 않는다 — 곡이 바뀔 때마다
 * 위젯 갱신이 돌아 폭주한다.
 */
data class NowPlayingSnapshot(
    val episodeId: Long?,
    val podcastId: Long?,
    val title: String,
    val feedTitle: String?,
    val imageUrl: String?,
    val isPlaying: Boolean,
    val stationId: String? = null,
    val stationArt: StationArt? = null,
) {
    val isStation: Boolean get() = stationId != null
}
