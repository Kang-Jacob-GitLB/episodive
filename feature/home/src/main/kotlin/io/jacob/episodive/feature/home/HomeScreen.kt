package io.jacob.episodive.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.jacob.episodive.core.designsystem.component.EpisodesSection
import io.jacob.episodive.core.designsystem.component.LoadingWheel
import io.jacob.episodive.core.designsystem.component.PodcastItem
import io.jacob.episodive.core.designsystem.component.SectionHeader
import io.jacob.episodive.core.designsystem.component.StateImage
import io.jacob.episodive.core.designsystem.component.SubSectionHeader
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Feed
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
import kotlin.time.Clock

@Composable
internal fun HomeRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onPodcastClick: (Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            onPodcastClick = onPodcastClick,
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
                        .padding(16.dp)
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
                            .fillMaxSize()
                            .padding(16.dp)
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
                            FollowedPodcastsSection(
                                title = stringResource(R.string.feature_home_section_followed_podcasts),
                                followedPodcasts = followedPodcasts,
                                onMore = {

                                },
                                onFollowedPodcastClick = { followedPodcast ->
                                    onPodcastClick(followedPodcast.podcast.id)
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

@Composable
private fun LoadingScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingWheel()
    }
}

@Composable
private fun ErrorScreen(
    modifier: Modifier = Modifier,
    message: String,
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message)
    }
}

@Composable
private fun PlayingEpisodesSection(
    modifier: Modifier = Modifier,
    playingEpisodes: List<PlayedEpisode>,
    onEpisodeClick: (PlayedEpisode) -> Unit,
) {
    SubSectionHeader(
        modifier = modifier,
        title = stringResource(R.string.feature_home_section_continue),
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                count = playingEpisodes.size,
                key = { playingEpisodes[it].episode.id }
            ) { index ->
                val playedEpisode = playingEpisodes[index]
                PlayingEpisodeItem(
                    playedEpisode = playedEpisode,
                    onClick = { onEpisodeClick(playedEpisode) }
                )
            }
        }
    }
}

@Composable
private fun PlayingEpisodeItem(
    modifier: Modifier = Modifier,
    playedEpisode: PlayedEpisode,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .size(width = 192.dp, height = 84.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StateImage(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(8.dp)),
                imageUrl = playedEpisode.episode.image.ifEmpty { playedEpisode.episode.feedImage },
                contentDescription = playedEpisode.episode.title,
            )

            Column(
                modifier = Modifier
                    .width(98.dp)
            ) {
                Text(
                    text = playedEpisode.episode.feedTitle ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

//                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = playedEpisode.episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.onSurface,
                    trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    gapSize = (-4).dp,
                    drawStopIndicator = {},
                    progress = {
                        val duration = playedEpisode.episode.duration?.toIntSeconds()
                        val position = playedEpisode.position.toIntSeconds()
                        if (duration != null && duration > 0) {
                            (position.toFloat() / duration).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun FeedsSection(
    modifier: Modifier = Modifier,
    title: String,
    feeds: List<Feed>,
    onFeedClick: (Feed) -> Unit,
) {
    SectionHeader(
        modifier = modifier,
        title = title,
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                count = feeds.size,
                key = { feeds[it].id }
            ) { index ->
                val feed = feeds[index]
                FeedItem(
                    feed = feed,
                    onClick = { onFeedClick(feed) }
                )
            }
        }
    }
}

@Composable
private fun FeedItem(
    modifier: Modifier = Modifier,
    feed: Feed,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .clickable { onClick() },
    ) {
        StateImage(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp)),
            imageUrl = feed.image ?: "",
            contentDescription = feed.title,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = feed.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = feed.author ?: "",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FollowedPodcastsSection(
    modifier: Modifier = Modifier,
    title: String,
    followedPodcasts: List<FollowedPodcast>,
    onMore: () -> Unit,
    onFollowedPodcastClick: (FollowedPodcast) -> Unit,
) {
    SectionHeader(
        modifier = modifier,
        title = title,
        actionIcon = EpisodiveIcons.KeyboardArrowRight,
        actionIconContentDescription = title,
        onActionClick = onMore
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                count = followedPodcasts.size,
                key = { followedPodcasts[it].podcast.id }
            ) { index ->
                val followedPodcast = followedPodcasts[index]
                PodcastItem(
                    podcast = followedPodcast.podcast,
                    onClick = { onFollowedPodcastClick(followedPodcast) }
                )
            }
        }
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