package io.jacob.episodive.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.common.combine
import io.jacob.episodive.core.domain.usecase.episode.GetLiveEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetMyRandomEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetPlayingEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.ResumeEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetChannelsUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetFollowedPodcastsUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetMyRecentPodcastsUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetMyTrendingPodcastsUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetTrendingPodcastsUseCase
import io.jacob.episodive.core.domain.usecase.user.GetUserDataUseCase
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    getUserDataUseCase: GetUserDataUseCase,
    getPlayingEpisodesUseCase: GetPlayingEpisodesUseCase,
    getMyRecentPodcastsUseCase: GetMyRecentPodcastsUseCase,
    getMyRandomEpisodesUseCase: GetMyRandomEpisodesUseCase,
    getMyTrendingPodcastsUseCase: GetMyTrendingPodcastsUseCase,
    getFollowedPodcastsUseCase: GetFollowedPodcastsUseCase,
    private val getTrendingPodcastsUseCase: GetTrendingPodcastsUseCase,
    getLiveEpisodesUseCase: GetLiveEpisodesUseCase,
    getChannelsUseCase: GetChannelsUseCase,
    private val playEpisodeUseCase: PlayEpisodeUseCase,
    private val resumeEpisodeUseCase: ResumeEpisodeUseCase,
    private val toggleLikedUseCase: ToggleLikedUseCase,
) : ViewModel() {

    private val localTrendingPodcasts = getUserDataUseCase().flatMapLatest { userData ->
        getTrendingPodcastsUseCase(language = userData.language)
    }

    private val foreignTrendingPodcasts = getUserDataUseCase().flatMapLatest { userData ->
        val foreignLanguages = languages.filter { it != userData.language }.joinToString(",")
        getTrendingPodcastsUseCase(language = foreignLanguages)
    }

    val state: StateFlow<HomeState> = combine(
        getPlayingEpisodesUseCase(),
        getMyRecentPodcastsUseCase(),
        getMyRandomEpisodesUseCase(),
        getMyTrendingPodcastsUseCase(),
        getFollowedPodcastsUseCase(),
        localTrendingPodcasts,
        foreignTrendingPodcasts,
        getLiveEpisodesUseCase(),
        getChannelsUseCase(),
    ) {
            playingEpisodes,
            myRecentPodcasts,
            randomEpisodes,
            myTrendingPodcasts,
            followedPodcasts,
            localTrendingPodcasts,
            foreignTrendingPodcasts,
            liveEpisodes,
            channels,
        ->

        HomeState.Success(
            playingEpisodes = playingEpisodes.take(10),
            myRecentPodcasts = myRecentPodcasts.take(10),
            randomEpisodes = randomEpisodes.take(6),
            myTrendingFPodcasts = myTrendingPodcasts.take(10),
            followedPodcasts = followedPodcasts.take(10),
            localTrendingPodcasts = localTrendingPodcasts.take(10),
            foreignTrendingPodcasts = foreignTrendingPodcasts.take(10),
            liveEpisodes = liveEpisodes.take(6),
            channels = channels
        ) as HomeState
    }.catch { e ->
        emit(HomeState.Error(e.message ?: "An unknown error occurred"))
        e.printStackTrace()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeState.Loading
    )

    private val _action = MutableSharedFlow<HomeAction>(extraBufferCapacity = 1)

    private val _effect = MutableSharedFlow<HomeEffect>(extraBufferCapacity = 1)
    val effect = _effect.asSharedFlow()

    init {
        handleActions()
    }

    private fun handleActions() = viewModelScope.launch {
        _action.collectLatest { action ->
            when (action) {
                is HomeAction.PlayEpisode -> playEpisode(action.episode)
                is HomeAction.ResumeEpisode -> resumeEpisode(action.playedEpisode)
                is HomeAction.ToggleEpisodeLiked -> toggleEpisodeLiked(action.episode)
                is HomeAction.ClickPodcast -> clickPodcast(action.podcastId)
                is HomeAction.ClickChannel -> clickChannel(action.channel)
            }
        }
    }

    fun sendAction(action: HomeAction) = viewModelScope.launch {
        _action.emit(action)
    }

    private fun playEpisode(episode: Episode) = viewModelScope.launch {
        playEpisodeUseCase(episode)
    }

    private fun resumeEpisode(playedEpisode: Episode) = viewModelScope.launch {
        resumeEpisodeUseCase(playedEpisode)
    }

    private fun toggleEpisodeLiked(episode: Episode) = viewModelScope.launch {
        toggleLikedUseCase(episode.id)
    }


    private fun clickPodcast(podcastId: Long) = viewModelScope.launch {
        _effect.emit(HomeEffect.NavigateToPodcast(podcastId))
    }

    private fun clickChannel(channel: Channel) = viewModelScope.launch {
        _effect.emit(HomeEffect.NavigateToChannel(channel))
    }

    companion object {
        private val languages = listOf("en", "es", "fr", "de", "it", "ja", "ko", "pt", "ru", "zh")
    }
}

sealed interface HomeState {
    data object Loading : HomeState
    data class Success(
        val playingEpisodes: List<Episode>,
        val myRecentPodcasts: List<Podcast>,
        val randomEpisodes: List<Episode>,
        val myTrendingFPodcasts: List<Podcast>,
        val followedPodcasts: List<Podcast>,
        val localTrendingPodcasts: List<Podcast>,
        val foreignTrendingPodcasts: List<Podcast>,
        val liveEpisodes: List<Episode>,
        val channels: List<Channel>,
    ) : HomeState

    data class Error(val message: String) : HomeState
}

sealed interface HomeAction {
    data class PlayEpisode(val episode: Episode) : HomeAction
    data class ResumeEpisode(val playedEpisode: Episode) : HomeAction
    data class ToggleEpisodeLiked(val episode: Episode) : HomeAction
    data class ClickPodcast(val podcastId: Long) : HomeAction
    data class ClickChannel(val channel: Channel) : HomeAction
}

sealed interface HomeEffect {
    data class NavigateToPodcast(val podcastId: Long) : HomeEffect
    data class NavigateToChannel(val channel: Channel) : HomeEffect
}