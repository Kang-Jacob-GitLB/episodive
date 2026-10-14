package io.jacob.episodive.core.data.widget

import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.domain.usecase.player.GetNowPlayingUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetPodcastUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetUserRecentPodcastsUseCase
import io.jacob.episodive.core.domain.usecase.station.ObserveOnAirProgramUseCase
import io.jacob.episodive.core.domain.widget.NowPlayingSnapshot
import io.jacob.episodive.core.domain.widget.PodcastSnapshot
import io.jacob.episodive.core.domain.widget.WidgetDataReader
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.art
import io.jacob.episodive.core.model.coverUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetDataReaderImpl @Inject constructor(
    private val getNowPlaying: GetNowPlayingUseCase,
    private val getPodcast: GetPodcastUseCase,
    private val getUserRecentPodcasts: GetUserRecentPodcastsUseCase,
    private val userRepository: UserRepository,
    private val episodeRepository: EpisodeRepository,
    @param:Player(EpisodivePlayers.Main) private val playerRepository: PlayerRepository,
    private val observeOnAirProgram: ObserveOnAirProgramUseCase,
) : WidgetDataReader {

    /**
     * 활성 재생 세션이 없을 때 보여줄 폴백 에피소드 = 미니플레이어가 복원하는 것과 동일한
     * "마지막 재생" 에피소드. 재생 버튼의 autoplay 도 [UserRepository.getLastPlayState] 출처를
     * 복원하므로(표시=재생 일치) 같은 소스를 쓴다.
     */
    private suspend fun lastPlayedEpisode() =
        userRepository.getLastPlayState()?.episodeId
            ?.let { episodeRepository.getEpisodeById(it).first() }

    /** [programTitle] 을 알면 방송사 대신 부제로 쓴다. */
    private fun Station.toSnapshot(isPlaying: Boolean, programTitle: String? = null) = NowPlayingSnapshot(
        episodeId = null,
        podcastId = null,
        title = name,
        feedTitle = programTitle ?: broadcaster,
        imageUrl = null,
        isPlaying = isPlaying,
        stationId = id,
        stationArt = art,
    )

    override suspend fun snapshotNowPlaying(): NowPlayingSnapshot? {
        // nowPlayingFlow 와 같은 판정이다 — 스테이션 여부는 nowPlayingStation 하나로만 가른다.
        playerRepository.nowPlayingStation.first()?.let { station ->
            return station.toSnapshot(playerRepository.isPlaying.first())
        }
        val active = getNowPlaying().first()
        val episode = active ?: lastPlayedEpisode() ?: return null
        // 폴백(마지막 재생)일 때는 일시정지 상태로 표시.
        val isPlaying = active != null && playerRepository.isPlaying.first()
        // Episode.feedTitle 이 비어있으면 팟캐스트를 따로 조회해 title 로 대체.
        val podcastName = episode.feedTitle?.takeIf { it.isNotBlank() }
            ?: getPodcast(episode.feedId).first()?.title
        return NowPlayingSnapshot(
            episodeId = episode.id,
            podcastId = episode.feedId,
            title = episode.title,
            feedTitle = podcastName,
            // 에피소드 자체 image 가 비어있으면 feed(팟캐스트) 이미지로 fallback.
            imageUrl = episode.coverUrl.ifBlank { null },
            isPlaying = isPlaying,
        )
    }

    override fun nowPlayingFlow(): Flow<NowPlayingSnapshot?> =
        // 스테이션 여부는 nowPlayingStation 하나로만 판정한다(nowPlaying 과 combine 하지 않는다).
        playerRepository.nowPlayingStation.flatMapLatest { station ->
            if (station != null) {
                // streamTitle·곡은 싣지 않는다. 곡마다 스냅샷이 바뀌어 위젯 갱신이 폭주한다. 프로그램 제목은 프로그램
                // 경계에서만 바뀌므로 싣는다. 재생 중인 항목(MediaItem)은 건드리지 않고 이 스냅샷만 갱신한다.
                // 네트워크로 받는 값이라 첫 값을 기다리지 않게 null 로 시작한다.
                combine(
                    playerRepository.isPlaying,
                    observeOnAirProgram(station.id).onStart { emit(null) },
                ) { isPlaying, program -> station.toSnapshot(isPlaying, program?.title?.takeIf { it.isNotBlank() }) }
            } else {
                episodeNowPlayingFlow()
            }
        }.distinctUntilChanged { old, new ->
            // 재생 위치(progress) 재방출 폭주 차단: 에피소드/스테이션/재생상태/팟캐스트명이 같으면 무시.
            // feedTitle 은 팟캐스트 비동기 로드로 뒤늦게 채워질 수 있으므로 비교에 포함한다.
            old?.episodeId == new?.episodeId &&
                old?.stationId == new?.stationId &&
                old?.isPlaying == new?.isPlaying &&
                old?.feedTitle == new?.feedTitle
        }

    private fun episodeNowPlayingFlow(): Flow<NowPlayingSnapshot?> =
        getNowPlaying().flatMapLatest { active ->
            // 활성 재생이 없으면 마지막 재생 에피소드로 폴백(미니플레이어처럼, 일시정지 상태로 표시).
            val episode = active ?: lastPlayedEpisode()
            if (episode == null) {
                flowOf(null)
            } else {
                // 에피소드별로 팟캐스트도 함께 구독해 feedTitle 이 비면 podcast.title 로 대체.
                combine(getPodcast(episode.feedId), playerRepository.isPlaying) { podcast, isPlaying ->
                    NowPlayingSnapshot(
                        episodeId = episode.id,
                        podcastId = episode.feedId,
                        title = episode.title,
                        feedTitle = episode.feedTitle?.takeIf { it.isNotBlank() } ?: podcast?.title,
                        imageUrl = episode.coverUrl.ifBlank { null },
                        isPlaying = active != null && isPlaying,
                    )
                }
            }
        }

    override fun userRecentPodcastsFlow(max: Int): Flow<List<PodcastSnapshot>> =
        getUserRecentPodcasts(max = max)
            .map { podcasts ->
                podcasts.map { podcast ->
                    PodcastSnapshot(
                        id = podcast.id,
                        title = podcast.title,
                        imageUrl = podcast.coverUrl.ifBlank { null },
                    )
                }
            }
            .distinctUntilChanged()
}
