package io.jacob.episodive.feature.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.domain.usecase.episode.GetEpisodesByPodcastIdPagingUseCase
import io.jacob.episodive.core.domain.usecase.episode.SaveEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetPodcastUseCase
import io.jacob.episodive.core.domain.usecase.podcast.ToggleFollowedUseCase
import io.jacob.episodive.core.model.DataError
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.asDataError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

@HiltViewModel(assistedFactory = PodcastViewModel.Factory::class)
class PodcastViewModel @AssistedInject constructor(
    getPodcastUseCase: GetPodcastUseCase,
    getEpisodesByPodcastIdPagingUseCase: GetEpisodesByPodcastIdPagingUseCase,
    private val toggleFollowedUseCase: ToggleFollowedUseCase,
    private val playEpisodeUseCase: PlayEpisodeUseCase,
    private val toggleLikedEpisodeUseCase: ToggleLikedEpisodeUseCase,
    private val saveEpisodeUseCase: SaveEpisodeUseCase,
    @Assisted("id") val id: Long,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(@Assisted("id") id: Long): PodcastViewModel
    }

    val episodesPaging = getEpisodesByPodcastIdPagingUseCase(id).cachedIn(viewModelScope)

    // 재시도는 소스 체인을 통째로 재구독해야 한다. getPodcastUseCase 는 콜드 Flow라
    // 재구독해도 부작용이 없다.
    private val retryTrigger = MutableStateFlow(0)

    val state: StateFlow<PodcastState> = retryTrigger.flatMapLatest {
        getPodcastUseCase(id)
            .map { podcast ->
                podcast?.let {
                    PodcastState.Success(
                        podcast = podcast,
                    )
                } ?: PodcastState.Error(DataError.NotFound)
            }.catch { e ->
                Timber.e(e, "팟캐스트 상세를 불러오지 못했다 (id=$id)")
                emit(PodcastState.Error(e.asDataError()))
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PodcastState.Loading
    )

    private val _action = MutableSharedFlow<PodcastAction>(extraBufferCapacity = 1)

    private val _effect = MutableSharedFlow<PodcastEffect>(extraBufferCapacity = 1)
    val effect = _effect.asSharedFlow()

    init {
        handleActions()
    }

    private fun handleActions() = viewModelScope.launch {
        _action.collectLatest { action ->
            when (action) {
                is PodcastAction.ToggleFollowed -> toggleFollowed()
                is PodcastAction.PlayEpisode -> playEpisode(action.episode, action.visibleEpisodes)
                is PodcastAction.ToggleLikedEpisode -> toggleLikedEpisode(action.episode)
                is PodcastAction.ToggleSavedEpisode -> toggleSavedEpisode(action.episode)
                is PodcastAction.Retry -> retry()
            }
        }
    }

    fun sendAction(action: PodcastAction) = viewModelScope.launch {
        _action.emit(action)
    }

    /**
     * 연타한 토글을 누른 순서대로 처리한다. 토글마다 따로 띄운 코루틴이 제각각 끝나면 이펙트가
     * 뒤바뀌어 도착할 수 있고, 화면은 마지막 이펙트의 스낵바만 남기므로(PodcastRoute) 실제 상태와
     * 반대로 말하는 스낵바가 남는다. 그 되돌리기는 엉뚱한 방향으로 토글한다.
     */
    private val followMutex = Mutex()

    private fun toggleFollowed() = viewModelScope.launch {
        followMutex.withLock {
            val isFollowedNow = toggleFollowedUseCase(id)
            _effect.emit(PodcastEffect.ShowFollowSnackbar(isFollowedNow))
        }
    }

    private fun playEpisode(episode: Episode, visibleEpisodes: List<Episode>) =
        viewModelScope.launch {
        val index = visibleEpisodes.indexOfFirst { it.id == episode.id }
        if (index == -1) {
            playEpisodeUseCase(playEpisode = episode, episodes = visibleEpisodes)
            return@launch
        }
        val playlist = visibleEpisodes.subList(0, index + 1).reversed()
            playEpisodeUseCase(playEpisode = episode, episodes = playlist)
    }

    private fun toggleLikedEpisode(episode: Episode) = viewModelScope.launch {
        toggleLikedEpisodeUseCase(episode)
    }

    private fun toggleSavedEpisode(episode: Episode) = viewModelScope.launch {
        val isSavedNow = saveEpisodeUseCase(episode)
        if (!isSavedNow) {
            _effect.emit(PodcastEffect.ShowUnsaveSnackbar(episode))
        }
    }

    private fun retry() {
        retryTrigger.update { it + 1 }
    }
}

sealed interface PodcastEffect {
    data class ShowFollowSnackbar(val isFollowed: Boolean) : PodcastEffect
    data class ShowUnsaveSnackbar(val episode: Episode) : PodcastEffect
}

sealed interface PodcastState {
    data object Loading : PodcastState
    data class Success(
        val podcast: Podcast,
    ) : PodcastState

    data class Error(val error: DataError) : PodcastState
}

sealed interface PodcastAction {
    data object ToggleFollowed : PodcastAction
    data class PlayEpisode(
        val episode: Episode,
        val visibleEpisodes: List<Episode>,
    ) : PodcastAction
    data class ToggleLikedEpisode(val episode: Episode) : PodcastAction
    data class ToggleSavedEpisode(val episode: Episode) : PodcastAction
    data object Retry : PodcastAction
}