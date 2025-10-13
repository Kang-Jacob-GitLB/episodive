package io.jacob.episodive.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.domain.usecase.episode.GetLiveEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetMyRandomEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetPlayingEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.feed.GetMyRecentFeedsUseCase
import io.jacob.episodive.core.domain.usecase.feed.GetMyTrendingFeedsUseCase
import io.jacob.episodive.core.domain.usecase.feed.GetTrendingFeedsUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.ResumeEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetFollowedPodcastsUseCase
import io.jacob.episodive.core.domain.usecase.user.GetUserDataUseCase
import io.jacob.episodive.core.domain.util.combine
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.FollowedPodcast
import io.jacob.episodive.core.model.PlayedEpisode
import io.jacob.episodive.core.model.RecentFeed
import io.jacob.episodive.core.model.TrendingFeed
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    getUserDataUseCase: GetUserDataUseCase,
    getPlayingEpisodesUseCase: GetPlayingEpisodesUseCase,
    getMyRecentFeedsUseCase: GetMyRecentFeedsUseCase,
    getMyRandomEpisodesUseCase: GetMyRandomEpisodesUseCase,
    getMyTrendingFeedsUseCase: GetMyTrendingFeedsUseCase,
    getFollowedPodcastsUseCase: GetFollowedPodcastsUseCase,
    private val getTrendingFeedsUseCase: GetTrendingFeedsUseCase,
    getLiveEpisodesUseCase: GetLiveEpisodesUseCase,
    private val playEpisodeUseCase: PlayEpisodeUseCase,
    private val resumeEpisodeUseCase: ResumeEpisodeUseCase,
) : ViewModel() {

    private val localTrendingFeeds = getUserDataUseCase().flatMapLatest { userData ->
        getTrendingFeedsUseCase(language = userData.language)
    }

    private val foreignTrendingFeeds = getUserDataUseCase().flatMapLatest { userData ->
        val foreignLanguages = languages.filter { it != userData.language }.joinToString(",")
        getTrendingFeedsUseCase(language = foreignLanguages)
    }

    val state: StateFlow<HomeState> = combine(
        getPlayingEpisodesUseCase(),
        getMyRecentFeedsUseCase(),
        getMyRandomEpisodesUseCase(),
        getMyTrendingFeedsUseCase(),
        getFollowedPodcastsUseCase(),
        localTrendingFeeds,
        foreignTrendingFeeds,
        getLiveEpisodesUseCase(),
    ) { playingEpisodes,
        myRecentFeeds,
        randomEpisodes,
        myTrendingFeeds,
        followedPodcasts,
        localTrendingFeeds,
        foreignTrendingFeeds,
        liveEpisodes ->

        HomeState.Success(
            playingEpisodes = playingEpisodes.take(10),
            myRecentFeeds = myRecentFeeds.take(10),
            randomEpisodes = randomEpisodes.take(6),
            myTrendingFeeds = myTrendingFeeds.take(10),
            followedPodcasts = followedPodcasts.take(10),
            localTrendingFeeds = localTrendingFeeds.take(10),
            foreignTrendingFeeds = foreignTrendingFeeds.take(10),
            liveEpisodes = liveEpisodes.take(6),
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

    init {
        handleActions()
    }

    private fun handleActions() = viewModelScope.launch {
        _action.collect { action ->
            when (action) {
                is HomeAction.PlayEpisode -> playEpisode(action.episode)
                is HomeAction.ResumeEpisode -> resumeEpisode(action.playedEpisode)
            }
        }
    }

    fun sendAction(action: HomeAction) = viewModelScope.launch {
        _action.emit(action)
    }

    private fun playEpisode(episode: Episode) = viewModelScope.launch {
        playEpisodeUseCase(episode)
    }

    private fun resumeEpisode(playedEpisode: PlayedEpisode) = viewModelScope.launch {
        resumeEpisodeUseCase(playedEpisode)
    }

    companion object {
        private val languages = listOf("en", "es", "fr", "de", "it", "ja", "ko", "pt", "ru", "zh")
    }
}

sealed interface HomeState {
    data object Loading : HomeState
    data class Success(
        val playingEpisodes: List<PlayedEpisode>,
        val myRecentFeeds: List<RecentFeed>,
        val randomEpisodes: List<Episode>,
        val myTrendingFeeds: List<TrendingFeed>,
        val followedPodcasts: List<FollowedPodcast>,
        val localTrendingFeeds: List<TrendingFeed>,
        val foreignTrendingFeeds: List<TrendingFeed>,
        val liveEpisodes: List<Episode>,
    ) : HomeState

    data class Error(val message: String) : HomeState
}

sealed interface HomeAction {
    data class PlayEpisode(val episode: Episode) : HomeAction
    data class ResumeEpisode(val playedEpisode: PlayedEpisode) : HomeAction
}