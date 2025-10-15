package io.jacob.episodive.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.domain.usecase.FindInLibraryUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetAllPlayedEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetLikedEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.ResumeEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetFollowedPodcastsUseCase
import io.jacob.episodive.core.domain.usecase.podcast.ToggleFollowedUseCase
import io.jacob.episodive.core.domain.usecase.user.GetPreferredCategoriesUseCase
import io.jacob.episodive.core.domain.usecase.user.ToggleCategoryUseCase
import io.jacob.episodive.core.domain.util.combine
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.FollowedPodcast
import io.jacob.episodive.core.model.LibraryFindResult
import io.jacob.episodive.core.model.LikedEpisode
import io.jacob.episodive.core.model.PlayedEpisode
import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val findInLibraryUseCase: FindInLibraryUseCase,
    getAllPlayedEpisodesUseCase: GetAllPlayedEpisodesUseCase,
    getLikedEpisodesUseCase: GetLikedEpisodesUseCase,
    getFollowedPodcastsUseCase: GetFollowedPodcastsUseCase,
    private val getPreferredCategoriesUseCase: GetPreferredCategoriesUseCase,
    private val playEpisodeUseCase: PlayEpisodeUseCase,
    private val resumeEpisodeUseCase: ResumeEpisodeUseCase,
    private val toggleLikedUseCase: ToggleLikedUseCase,
    private val toggleFollowedUseCase: ToggleFollowedUseCase,
    private val toggleCategoryUseCase: ToggleCategoryUseCase,
) : ViewModel() {
    private val _findQuery = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    private val _findResult: Flow<LibraryFindResult> = _findQuery
        .debounce(500L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isNotEmpty()) {
                findInLibraryUseCase(query)
            } else {
                flowOf(LibraryFindResult())
            }
        }

    val state: StateFlow<LibraryState> = combine(
        _findQuery,
        _findResult,
        getAllPlayedEpisodesUseCase(),
        getLikedEpisodesUseCase(),
        getFollowedPodcastsUseCase(),
    ) { query, result, allPlayedEpisodes, likedEpisodes, followedPodcasts ->
        LibraryState.Success(
            findQuery = query,
            findResult = result,
            allPlayedEpisodes = allPlayedEpisodes,
            likedEpisodes = likedEpisodes,
            followedPodcasts = followedPodcasts
        ) as LibraryState
    }.catch { e ->
        emit(LibraryState.Error(e.message ?: "An unknown error occurred"))
        e.printStackTrace()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryState.Loading,
    )

    private val _action = MutableSharedFlow<LibraryAction>(extraBufferCapacity = 1)

    private val _effect = MutableSharedFlow<LibraryEffect>(extraBufferCapacity = 1)
    val effect = _effect.asSharedFlow()

    init {
        handleActions()
    }

    private fun handleActions() = viewModelScope.launch {
        _action.collect { action ->
            when (action) {
                is LibraryAction.QueryChanged -> changeQuery(action.query)
                is LibraryAction.ClickFind -> changeQuery(action.query)
                is LibraryAction.ClearQuery -> clearQuery()
                is LibraryAction.ClickPlayingEpisode -> resumeEpisode(action.playingEpisode)
                is LibraryAction.ClickEpisode -> playEpisode(action.episode)
                is LibraryAction.ClickPodcast -> clickPodcast(action.podcast)
                is LibraryAction.ToggleLikedEpisode -> toggleLikedEpisode(action.likedEpisode)
                is LibraryAction.ToggleFollowedPodcast -> toggleFollowedPodcast(action.followedPodcast)
            }
        }
    }

    fun sendAction(action: LibraryAction) = viewModelScope.launch {
        _action.emit(action)
    }

    private fun changeQuery(query: String) = viewModelScope.launch {
        _findQuery.emit(query)
    }

    private fun clearQuery() = viewModelScope.launch {
        _findQuery.emit("")
    }

    private fun resumeEpisode(playedEpisode: PlayedEpisode) {
        resumeEpisodeUseCase(playedEpisode)
    }

    private fun playEpisode(episode: Episode) {
        playEpisodeUseCase(episode)
    }

    private fun clickPodcast(podcast: Podcast) = viewModelScope.launch {
        _effect.emit(LibraryEffect.NavigateToPodcast(podcast))
    }

    private fun toggleLikedEpisode(likedEpisode: LikedEpisode) = viewModelScope.launch {
        toggleLikedUseCase(likedEpisode.episode.id)
    }

    private fun toggleFollowedPodcast(followedPodcast: FollowedPodcast) = viewModelScope.launch {
        toggleFollowedUseCase(followedPodcast.podcast.id)
    }
}

sealed interface LibraryState {
    data object Loading : LibraryState
    data class Success(
        val findQuery: String,
        val findResult: LibraryFindResult,
        val allPlayedEpisodes: List<PlayedEpisode>,
        val likedEpisodes: List<LikedEpisode>,
        val followedPodcasts: List<FollowedPodcast>,
    ) : LibraryState

    data class Error(val message: String) : LibraryState
}

sealed interface LibraryAction {
    data class QueryChanged(val query: String) : LibraryAction
    data class ClickFind(val query: String) : LibraryAction
    data object ClearQuery : LibraryAction
    data class ClickPlayingEpisode(val playingEpisode: PlayedEpisode) : LibraryAction
    data class ClickEpisode(val episode: Episode) : LibraryAction
    data class ClickPodcast(val podcast: Podcast) : LibraryAction
    data class ToggleLikedEpisode(val likedEpisode: LikedEpisode) : LibraryAction
    data class ToggleFollowedPodcast(val followedPodcast: FollowedPodcast) : LibraryAction
}

sealed interface LibraryEffect {
    data class NavigateToPodcast(val podcast: Podcast) : LibraryEffect
}