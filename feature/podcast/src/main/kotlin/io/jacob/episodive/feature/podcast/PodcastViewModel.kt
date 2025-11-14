package io.jacob.episodive.feature.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.domain.usecase.episode.GetEpisodesByPodcastIdUseCase
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedUseCase
import io.jacob.episodive.core.domain.usecase.image.GetDominantColorFromUrlUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetPodcastUseCase
import io.jacob.episodive.core.domain.usecase.podcast.ToggleFollowedUseCase
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = PodcastViewModel.Factory::class)
class PodcastViewModel @AssistedInject constructor(
    getPodcastUseCase: GetPodcastUseCase,
    getEpisodesByPodcastIdUseCase: GetEpisodesByPodcastIdUseCase,
    private val toggleFollowedUseCase: ToggleFollowedUseCase,
    private val playEpisodesUseCase: PlayEpisodesUseCase,
    private val toggleLikedUseCase: ToggleLikedUseCase,
    private val getDominantColorFromUrlUseCase: GetDominantColorFromUrlUseCase,
    @Assisted("id") val id: Long,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(@Assisted("id") id: Long): PodcastViewModel
    }

    private val dominantColor = getPodcastUseCase(id).mapNotNull { it }
        .flatMapLatest { podcast ->
            val color = getDominantColorFromUrlUseCase(podcast.image)
            flowOf(color)
        }

    val state: StateFlow<PodcastState> = combine(
        getPodcastUseCase(id),
        getEpisodesByPodcastIdUseCase(id),
        dominantColor,
    ) { podcast, episodes, dominantColor ->
        if (podcast == null) {
            PodcastState.Error("Podcast not found")
        } else {
            PodcastState.Success(
                podcast = podcast,
                episodes = episodes,
                dominantColor = dominantColor,
            )
        }
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
                is PodcastAction.PlayEpisode -> playEpisode(action.episode)
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

    private fun playEpisode(episode: Episode) {
        val state = state.value
        if (state !is PodcastState.Success) return
        val indexToPlay = state.episodes.indexOfFirst { it.id == episode.id }
        if (indexToPlay == -1) return
        val episodes = state.episodes.subList(0, indexToPlay + 1).reversed()
        playEpisodesUseCase(episodes = episodes, playEpisode = episode)
    }

    private fun toggleLikedEpisode(episode: Episode) = viewModelScope.launch {
        toggleLikedUseCase(episode.id)
    }
}

sealed interface PodcastState {
    data object Loading : PodcastState
    data class Success(
        val podcast: Podcast,
        val episodes: List<Episode>,
        val dominantColor: ULong,
    ) : PodcastState

    data class Error(val message: String) : PodcastState
}

sealed interface PodcastAction {
    data object ToggleFollowed : PodcastAction
    data class PlayEpisode(val episode: Episode) : PodcastAction
    data class ToggleLikedEpisode(val episode: Episode) : PodcastAction
}