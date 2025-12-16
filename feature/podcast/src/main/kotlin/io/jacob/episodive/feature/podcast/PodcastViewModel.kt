package io.jacob.episodive.feature.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.domain.usecase.episode.GetEpisodesByPodcastIdPagingUseCase
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetPodcastUseCase
import io.jacob.episodive.core.domain.usecase.podcast.ToggleFollowedUseCase
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = PodcastViewModel.Factory::class)
class PodcastViewModel @AssistedInject constructor(
    getPodcastUseCase: GetPodcastUseCase,
    getEpisodesByPodcastIdPagingUseCase: GetEpisodesByPodcastIdPagingUseCase,
    private val toggleFollowedUseCase: ToggleFollowedUseCase,
    private val playEpisodesUseCase: PlayEpisodesUseCase,
    private val toggleLikedUseCase: ToggleLikedUseCase,
    @Assisted("id") val id: Long,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(@Assisted("id") id: Long): PodcastViewModel
    }

    val episodesPaging = getEpisodesByPodcastIdPagingUseCase(id).cachedIn(viewModelScope)

    val state: StateFlow<PodcastState> = getPodcastUseCase(id)
        .map { podcast ->
            podcast?.let {
                PodcastState.Success(
                    podcast = podcast,
                )
            } ?: PodcastState.Error("Podcast not found")
        }.catch { e ->
            emit(PodcastState.Error(e.message ?: "Unknown error"))
            e.printStackTrace()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PodcastState.Loading
        )

    private val _action = MutableSharedFlow<PodcastAction>(extraBufferCapacity = 1)

    init {
        handleActions()
    }

    private fun handleActions() = viewModelScope.launch {
        _action.collectLatest { action ->
            when (action) {
                is PodcastAction.ToggleFollowed -> toggleFollowed()
                is PodcastAction.PlayEpisode -> playEpisode(action.episode, action.visibleEpisodes)
                is PodcastAction.ToggleLikedEpisode -> toggleLikedEpisode(action.episode)
            }
        }
    }

    fun sendAction(action: PodcastAction) = viewModelScope.launch {
        _action.emit(action)
    }

    private fun toggleFollowed() = viewModelScope.launch {
        toggleFollowedUseCase(id)
    }

    private fun playEpisode(episode: Episode, visibleEpisodes: List<Episode>) {
        val index = visibleEpisodes.indexOfFirst { it.id == episode.id }
        if (index == -1) {
            playEpisodesUseCase(playEpisode = episode, episodes = visibleEpisodes)
            return
        }
        val playlist = visibleEpisodes.subList(0, index + 1).reversed()
        playEpisodesUseCase(playEpisode = episode, episodes = playlist)
    }

    private fun toggleLikedEpisode(episode: Episode) = viewModelScope.launch {
        toggleLikedUseCase(episode.id)
    }
}

sealed interface PodcastState {
    data object Loading : PodcastState
    data class Success(
        val podcast: Podcast,
    ) : PodcastState

    data class Error(val message: String) : PodcastState
}

sealed interface PodcastAction {
    data object ToggleFollowed : PodcastAction
    data class PlayEpisode(
        val episode: Episode,
        val visibleEpisodes: List<Episode>,
    ) : PodcastAction
    data class ToggleLikedEpisode(val episode: Episode) : PodcastAction
}