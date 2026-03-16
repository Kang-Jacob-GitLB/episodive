package io.jacob.episodive.feature.podcast

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import io.jacob.episodive.core.designsystem.component.DominantRegion
import io.jacob.episodive.core.designsystem.component.EpisodiveButton
import io.jacob.episodive.core.designsystem.component.EpisodiveGradientBackground
import io.jacob.episodive.core.designsystem.component.FadeTopBarLayout
import io.jacob.episodive.core.designsystem.component.HtmlTextContainer
import io.jacob.episodive.core.designsystem.component.StateImage
import io.jacob.episodive.core.designsystem.component.scrollbar.DraggableScrollbar
import io.jacob.episodive.core.designsystem.component.scrollbar.scrollbarState
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.screen.ErrorScreen
import io.jacob.episodive.core.designsystem.screen.LoadingScreen
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.GradientColors
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.ui.EpisodeItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import io.jacob.episodive.core.ui.R as uiR

@Composable
internal fun PodcastRoute(
    modifier: Modifier = Modifier,
    viewModel: PodcastViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val s = state) {
        is PodcastState.Loading -> LoadingScreen()

        is PodcastState.Success -> {
            PodcastScreen(
                modifier = modifier,
                podcast = s.podcast,
                episodes = viewModel.episodesPaging,
                onFollowClick = { viewModel.sendAction(PodcastAction.ToggleFollowed) },
                onEpisodeClick = { episode, visibleEpisodes ->
                    viewModel.sendAction(PodcastAction.PlayEpisode(episode, visibleEpisodes))
                },
                onToggleLikedEpisode = { viewModel.sendAction(PodcastAction.ToggleLikedEpisode(it)) },
                onBackClick = onBackClick,
                onShowSnackbar = onShowSnackbar
            )
        }

        is PodcastState.Error -> ErrorScreen(message = s.message)
    }
}

@Composable
internal fun PodcastScreen(
    modifier: Modifier = Modifier,
    podcast: Podcast,
    episodes: Flow<PagingData<Episode>>,
    onFollowClick: () -> Unit,
    onEpisodeClick: (Episode, List<Episode>) -> Unit,
    onToggleLikedEpisode: (Episode) -> Unit,
    onBackClick: () -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    val episodesPaging = episodes.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    FadeTopBarLayout(
        modifier = modifier,
        state = listState,
        offset = 900,
        title = podcast.title,
        onBack = onBackClick
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PodcastHeader(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    podcast = podcast,
                    onFollowClick = onFollowClick,
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            item {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.feature_podcast_all_episodes_format).format(
                        podcast.episodeCount
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            items(
                count = episodesPaging.itemCount,
                key = { episodesPaging.peek(it)?.id ?: it },
                contentType = { "episode" }
            ) { index ->
                episodesPaging[index]?.let { episode ->
                    EpisodeItem(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        episode = episode,
                        onClick = {
                            val visibleEpisodes = episodesPaging.itemSnapshotList.items
                            onEpisodeClick(episode, visibleEpisodes)
                        },
                        onToggleLiked = { onToggleLikedEpisode(episode) },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(LocalDimensionTheme.current.playerBarHeight))
            }
        }

        listState.DraggableScrollbar(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 12.dp)
                .padding(top = 110.dp)
                .align(Alignment.TopEnd),
            state = listState.scrollbarState(itemsAvailable = episodesPaging.itemCount),
            orientation = Orientation.Vertical,
            onThumbMoved = { thumbPosition ->
                scope.launch {
                    val itemIndex = (thumbPosition * episodesPaging.itemCount).toInt()
                        .coerceIn(0, episodesPaging.itemCount - 1)
                    listState.scrollToItem(itemIndex)
                }
            }
        )
    }
}

@Composable
private fun PodcastHeader(
    modifier: Modifier = Modifier,
    podcast: Podcast,
    onFollowClick: () -> Unit,
) {
    val isFollowed = podcast.isFollowed
    var dominantColor by remember { mutableStateOf(Color.DarkGray) }

    EpisodiveGradientBackground(
        gradientColors = GradientColors(
            top = dominantColor,
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StateImage(
                modifier = Modifier
                    .size(220.dp)
                    .clip(shape = MaterialTheme.shapes.extraExtraLarge),
                imageUrl = podcast.image,
                contentDescription = podcast.title,
                onDominantColorExtracted = { dominantColor = it },
                dominantRegion = DominantRegion.Top,
                brightnessAdjustment = -0.2f
            )

            Text(
                text = podcast.author,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )

            Text(
                text = podcast.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            EpisodiveButton(
                onClick = onFollowClick,
                shape = MaterialTheme.shapes.large,
                buttonColors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
                ),
                text = { Text(stringResource(if (isFollowed) uiR.string.core_ui_unfollow else uiR.string.core_ui_follow)) },
                leadingIcon = {
                    Icon(
                        imageVector = if (isFollowed) EpisodiveIcons.PersonRemove else EpisodiveIcons.PersonAdd,
                        contentDescription = null
                    )
                },
            )

            HtmlTextContainer(text = podcast.description) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@DevicePreviews
@Composable
private fun PodcastScreenPreview() {
    EpisodiveTheme {
        PodcastScreen(
            podcast = podcastTestData,
            episodes = flowOf(PagingData.from(episodeTestDataList)),
            onFollowClick = {},
            onEpisodeClick = { _, _ -> },
            onToggleLikedEpisode = {},
            onBackClick = {},
            onShowSnackbar = { _, _ -> false }
        )
    }
}