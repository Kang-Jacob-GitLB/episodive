package io.jacob.episodive.feature.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.jacob.episodive.core.designsystem.component.EpisodiveDragHandle
import io.jacob.episodive.core.designsystem.component.EpisodiveTopAppBar
import io.jacob.episodive.core.designsystem.screen.ErrorScreen
import io.jacob.episodive.core.designsystem.screen.LoadingScreen
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.testing.model.channelTestDataList
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.liveEpisodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.ui.ChannelSection
import io.jacob.episodive.core.ui.EpisodesSection
import io.jacob.episodive.core.ui.PlayingEpisodesSection
import io.jacob.episodive.core.ui.PodcastsSection
import io.jacob.episodive.core.ui.PodcastsWithAuthorSection
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun HomeRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onPodcastClick: (Long) -> Unit,
    onChannelClick: (Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is HomeEffect.NavigateToPodcast -> onPodcastClick(effect.podcastId)
                is HomeEffect.NavigateToChannel -> onChannelClick(effect.channelId)
            }
        }
    }

    when (val s = state) {
        is HomeState.Loading -> LoadingScreen()

        is HomeState.Success -> HomeScreen(
            modifier = modifier
                .fillMaxSize(),
            playingEpisodes = s.playingEpisodes,
            myRecentPodcasts = s.myRecentPodcasts,
            randomEpisodes = s.randomEpisodes,
            myTrendingPodcasts = s.myTrendingFPodcasts,
            followedPodcasts = s.followedPodcasts,
            localTrendingPodcasts = s.localTrendingPodcasts,
            foreignTrendingPodcasts = s.foreignTrendingPodcasts,
            liveEpisodes = s.liveEpisodes,
            channels = s.channels,
            onPlayEpisode = { viewModel.sendAction(HomeAction.PlayEpisode(it)) },
            onResumeEpisode = { viewModel.sendAction(HomeAction.ResumeEpisode(it)) },
            onToggleEpisodeLiked = { viewModel.sendAction(HomeAction.ToggleEpisodeLiked(it)) },
            onPodcastClick = { viewModel.sendAction(HomeAction.ClickPodcast(it)) },
            onChannelClick = { viewModel.sendAction(HomeAction.ClickChannel(it)) },
        )

        is HomeState.Error -> ErrorScreen(message = s.message)
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    playingEpisodes: List<Episode>,
    myRecentPodcasts: List<Podcast>,
    randomEpisodes: List<Episode>,
    myTrendingPodcasts: List<Podcast>,
    followedPodcasts: List<Podcast>,
    localTrendingPodcasts: List<Podcast>,
    foreignTrendingPodcasts: List<Podcast>,
    liveEpisodes: List<Episode>,
    channels: List<Channel>,
    onPlayEpisode: (Episode) -> Unit,
    onResumeEpisode: (Episode) -> Unit,
    onToggleEpisodeLiked: (Episode) -> Unit,
    onPodcastClick: (Long) -> Unit,
    onChannelClick: (Long) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenHeight = this.maxHeight

        val density = LocalDensity.current
        var topBarHeight by remember { mutableStateOf(80.dp) }
        var contentHeight by remember { mutableStateOf(10.dp) }

        val sheetExpandHeight = screenHeight - topBarHeight - 32.dp
        val sheetPartiallyExpandHeight = screenHeight - topBarHeight - contentHeight - 32.dp

        val sheetState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded,
                skipHiddenState = true
            )
        )

        BottomSheetScaffold(
            modifier = Modifier.fillMaxSize(),
            scaffoldState = sheetState,
            topBar = {
                EpisodiveTopAppBar(
                    modifier = Modifier.onSizeChanged { size ->
                        topBarHeight = with(density) { size.height.toDp() }
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.feature_home_title),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    },
                )
            },
            content = {
                Column(
                    modifier = Modifier
                        .animateContentSize()
                        .onSizeChanged { size ->
                            contentHeight = with(density) { size.height.toDp() }
                        }
                ) {
                    if (playingEpisodes.isNotEmpty()) {
                        PlayingEpisodesSection(
                            playingEpisodes = playingEpisodes,
                            onEpisodeClick = onResumeEpisode
                        )
                    }
                }
            },
            sheetPeekHeight = sheetPartiallyExpandHeight,
            sheetDragHandle = { EpisodiveDragHandle() },
            sheetContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = sheetExpandHeight)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                    ) {
                        itemWithDivider {
                            PodcastsWithAuthorSection(
                                title = stringResource(R.string.feature_home_section_my_recent_feeds),
                                podcasts = myRecentPodcasts,
                                onPodcastClick = { feed ->
                                    onPodcastClick(feed.id)
                                }
                            )
                        }

                        itemWithDivider {
                            EpisodesSection(
                                title = stringResource(R.string.feature_home_section_random_episodes),
                                episodes = randomEpisodes,
                                onEpisodeClick = onPlayEpisode,
                                onToggleEpisodeLiked = onToggleEpisodeLiked
                            )
                        }

                        itemWithDivider {
                            PodcastsWithAuthorSection(
                                title = stringResource(R.string.feature_home_section_my_trending_feeds),
                                podcasts = myTrendingPodcasts,
                                onPodcastClick = { feed ->
                                    onPodcastClick(feed.id)
                                }
                            )
                        }

                        itemWithDivider {
                            PodcastsSection(
                                title = stringResource(R.string.feature_home_section_followed_podcasts),
                                podcasts = followedPodcasts,
                                onMore = {

                                },
                                onPodcastClick = { podcast ->
                                    onPodcastClick(podcast.id)
                                }
                            )
                        }

                        itemWithDivider {
                            PodcastsWithAuthorSection(
                                title = stringResource(R.string.feature_home_section_trending_in_local),
                                podcasts = localTrendingPodcasts,
                                onPodcastClick = { feed ->
                                    onPodcastClick(feed.id)
                                }
                            )
                        }

                        itemWithDivider {
                            PodcastsWithAuthorSection(
                                title = stringResource(R.string.feature_home_section_trending_in_foreign),
                                podcasts = foreignTrendingPodcasts,
                                onPodcastClick = { feed ->
                                    onPodcastClick(feed.id)
                                }
                            )
                        }

                        item {
                            EpisodesSection(
                                title = stringResource(R.string.feature_home_section_live_episodes),
                                episodes = liveEpisodes,
                                onEpisodeClick = onPlayEpisode,
                                onToggleEpisodeLiked = onToggleEpisodeLiked,
                            )
                        }

                        item {
                            ChannelSection(
                                title = stringResource(R.string.feature_home_section_channels),
                                channels = channels,
                                onChannelClick = onChannelClick
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

fun LazyListScope.itemWithDivider(
    key: Any? = null,
    contentType: Any? = null,
    content: @Composable LazyItemScope.() -> Unit,
) {
    item(key, contentType, content)
    item {
        HorizontalDivider(modifier = Modifier.padding(16.dp))
    }
}

@DevicePreviews
@Composable
private fun HomeScreenPreview() {
    EpisodiveTheme {
        HomeScreen(
            playingEpisodes = episodeTestDataList,
            myRecentPodcasts = podcastTestDataList,
            randomEpisodes = episodeTestDataList,
            myTrendingPodcasts = podcastTestDataList,
            followedPodcasts = podcastTestDataList,
            localTrendingPodcasts = podcastTestDataList,
            foreignTrendingPodcasts = podcastTestDataList,
            liveEpisodes = liveEpisodeTestDataList,
            channels = channelTestDataList,
            onPlayEpisode = {},
            onResumeEpisode = {},
            onToggleEpisodeLiked = {},
            onPodcastClick = {},
            onChannelClick = {},
        )
    }
}