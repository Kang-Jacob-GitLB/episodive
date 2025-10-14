package io.jacob.episodive.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.jacob.episodive.core.designsystem.component.EpisodesSection
import io.jacob.episodive.core.designsystem.component.FeedsSection
import io.jacob.episodive.core.designsystem.component.PlayingEpisodesSection
import io.jacob.episodive.core.designsystem.component.PodcastsSection
import io.jacob.episodive.core.designsystem.component.SectionHeader
import io.jacob.episodive.core.designsystem.screen.ErrorScreen
import io.jacob.episodive.core.designsystem.screen.LoadingScreen
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.FollowedPodcast
import io.jacob.episodive.core.model.PlayedEpisode
import io.jacob.episodive.core.model.RecentFeed
import io.jacob.episodive.core.model.TrendingFeed
import io.jacob.episodive.core.model.mapper.toDurationSeconds
import io.jacob.episodive.core.model.mapper.toFeedsFromRecent
import io.jacob.episodive.core.model.mapper.toFeedsFromTrending
import io.jacob.episodive.core.model.mapper.toIntSeconds
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.liveEpisodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.testing.model.recentFeedTestDataList
import io.jacob.episodive.core.testing.model.trendingFeedTestDataList
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Clock

@Composable
internal fun HomeRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onPodcastClick: (Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is HomeEffect.NavigateToPodcast -> onPodcastClick(effect.podcastId)
            }
        }
    }

    when (val s = state) {
        is HomeState.Loading -> LoadingScreen()

        is HomeState.Success -> HomeScreen(
            modifier = modifier
                .fillMaxSize(),
            playingEpisodes = s.playingEpisodes,
            myRecentFeeds = s.myRecentFeeds,
            randomEpisodes = s.randomEpisodes,
            myTrendingFeeds = s.myTrendingFeeds,
            followedPodcasts = s.followedPodcasts,
            localTrendingFeeds = s.localTrendingFeeds,
            foreignTrendingFeeds = s.foreignTrendingFeeds,
            liveEpisodes = s.liveEpisodes,
            onPlayEpisode = { viewModel.sendAction(HomeAction.PlayEpisode(it)) },
            onResumeEpisode = { viewModel.sendAction(HomeAction.ResumeEpisode(it)) },
            onPodcastClick = { viewModel.sendAction(HomeAction.ClickPodcast(it)) },
        )

        is HomeState.Error -> ErrorScreen(message = s.message)
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    playingEpisodes: List<PlayedEpisode>,
    myRecentFeeds: List<RecentFeed>,
    randomEpisodes: List<Episode>,
    myTrendingFeeds: List<TrendingFeed>,
    followedPodcasts: List<FollowedPodcast>,
    localTrendingFeeds: List<TrendingFeed>,
    foreignTrendingFeeds: List<TrendingFeed>,
    liveEpisodes: List<Episode>,
    onPlayEpisode: (Episode) -> Unit = {},
    onResumeEpisode: (PlayedEpisode) -> Unit = {},
    onPodcastClick: (Long) -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = this.maxWidth
        val screenHeight = this.maxHeight
        val aspectRatio = 4f / 3f
        val imageHeight = screenWidth / aspectRatio
        val sheetMinHeight = screenHeight - imageHeight + 50.dp

        val sheetState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded
            )
        )
        val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

        BottomSheetScaffold(
            modifier = Modifier.fillMaxSize(),
            scaffoldState = sheetState,
            content = {
                SectionHeader(
                    modifier = Modifier
                        .padding(top = systemBarsPadding.calculateTopPadding()),
                    title = stringResource(R.string.feature_home_title),
                ) {
                    if (playingEpisodes.isNotEmpty()) {
                        PlayingEpisodesSection(
                            playingEpisodes = playingEpisodes,
                            onEpisodeClick = onResumeEpisode
                        )
                    }
                }
            },
            sheetPeekHeight = sheetMinHeight,
            sheetDragHandle = {},
//            sheetContainerColor = MaterialTheme.colorScheme.surface,
            sheetContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = sheetMinHeight) // 실제 이미지 높이 반영
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                    ) {
                        item {
                            FeedsSection(
                                title = stringResource(R.string.feature_home_section_my_recent_feeds),
                                feeds = myRecentFeeds.toFeedsFromRecent(),
                                onFeedClick = { feed ->
                                    onPodcastClick(feed.id)
                                }
                            )
                        }

                        item {
                            EpisodesSection(
                                title = stringResource(R.string.feature_home_section_random_episodes),
                                episodes = randomEpisodes,
                                onEpisodeClick = onPlayEpisode
                            )
                        }

                        item {
                            FeedsSection(
                                title = stringResource(R.string.feature_home_section_my_trending_feeds),
                                feeds = myTrendingFeeds.toFeedsFromTrending(),
                                onFeedClick = { feed ->
                                    onPodcastClick(feed.id)
                                }
                            )
                        }

                        item {
                            PodcastsSection(
                                title = stringResource(R.string.feature_home_section_followed_podcasts),
                                podcasts = followedPodcasts.map { it.podcast },
                                onMore = {

                                },
                                onPodcastClick = { podcast ->
                                    onPodcastClick(podcast.id)
                                }
                            )
                        }

                        item {
                            FeedsSection(
                                title = stringResource(R.string.feature_home_section_trending_in_local),
                                feeds = localTrendingFeeds.toFeedsFromTrending(),
                                onFeedClick = { feed ->
                                    onPodcastClick(feed.id)
                                }
                            )
                        }

                        item {
                            FeedsSection(
                                title = stringResource(R.string.feature_home_section_trending_in_foreign),
                                feeds = foreignTrendingFeeds.toFeedsFromTrending(),
                                onFeedClick = { feed ->
                                    onPodcastClick(feed.id)
                                }
                            )
                        }

                        item {
                            EpisodesSection(
                                title = stringResource(R.string.feature_home_section_live_episodes),
                                episodes = liveEpisodes,
                                onEpisodeClick = onPlayEpisode
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(LocalDimensionTheme.current.playerBarHeight))
                        }
                    }
                }
            },
        )
    }
}

@DevicePreviews
@Composable
private fun HomeScreenPreview() {
    EpisodiveTheme {
        HomeScreen(
            playingEpisodes = episodeTestDataList.map {
                PlayedEpisode(
                    episode = it,
                    playedAt = Clock.System.now(),
                    position = (it.duration?.toIntSeconds()?.let { seconds -> seconds / 2 }
                        ?: 0).toDurationSeconds(),
                    isCompleted = false,
                )
            },
            myRecentFeeds = recentFeedTestDataList,
            randomEpisodes = episodeTestDataList,
            myTrendingFeeds = trendingFeedTestDataList,
            followedPodcasts = podcastTestDataList.map {
                FollowedPodcast(
                    podcast = it,
                    followedAt = Clock.System.now(),
                    isNotificationEnabled = false,
                )
            },
            localTrendingFeeds = trendingFeedTestDataList,
            foreignTrendingFeeds = trendingFeedTestDataList,
            liveEpisodes = liveEpisodeTestDataList,
            onPodcastClick = {},
        )
    }
}