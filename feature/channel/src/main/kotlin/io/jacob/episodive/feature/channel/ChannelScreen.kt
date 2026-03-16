package io.jacob.episodive.feature.channel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.jacob.episodive.core.designsystem.component.EpisodiveGradientBackground
import io.jacob.episodive.core.designsystem.component.EpisodiveIconText
import io.jacob.episodive.core.designsystem.component.FadeTopBarLayout
import io.jacob.episodive.core.designsystem.component.SectionHeader
import io.jacob.episodive.core.designsystem.component.StateImage
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.screen.ErrorScreen
import io.jacob.episodive.core.designsystem.screen.LoadingScreen
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.GradientColors
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.testing.model.channelTestData
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.ui.PodcastItem
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun ChannelRoute(
    modifier: Modifier = Modifier,
    viewModel: ChannelViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onPodcastClick: (Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ChannelEffect.NavigateBack -> onBackClick()
                is ChannelEffect.NavigateToPodcast -> onPodcastClick(effect.podcastId)
            }
        }
    }

    when (val s = state) {
        is ChannelState.Loading -> LoadingScreen()

        is ChannelState.Success -> {
            ChannelScreen(
                modifier = modifier,
                channel = s.channel,
                podcasts = s.podcasts,
                onBackClick = { viewModel.sendAction(ChannelAction.ClickBack) },
                onPodcastClick = { viewModel.sendAction(ChannelAction.ClickPodcast(it)) },
            )
        }

        is ChannelState.Error -> ErrorScreen(message = s.message)
    }
}

@Composable
internal fun ChannelScreen(
    modifier: Modifier = Modifier,
    channel: Channel,
    podcasts: List<Podcast>,
    onBackClick: () -> Unit,
    onPodcastClick: (Long) -> Unit,
) {
    val lazyGridState = rememberLazyGridState()
    val scrollProgress by remember {
        derivedStateOf {
            val maxOffset = 600f // 희미해지기 시작하는 최대 오프셋 값
            val firstVisibleItem = lazyGridState.firstVisibleItemIndex
            val scrollOffset = lazyGridState.firstVisibleItemScrollOffset.toFloat()
            if (firstVisibleItem == 0) {
                1f - (scrollOffset / maxOffset).coerceIn(0f, 1f)
            } else 0f
        }
    }

    FadeTopBarLayout(
        modifier = modifier,
        state = lazyGridState,
        offset = 300,
        title = channel.title,
        onBack = onBackClick
    ) {
        StateImage(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .alpha(scrollProgress)
                .scale(1f + (1f - scrollProgress) * .1f),
            imageUrl = channel.image,
            contentDescription = channel.title,
        )

        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize(),
            state = lazyGridState,
            columns = GridCells.Fixed(2),
        ) {
            item(span = { GridItemSpan(2) }) {
                ChannelHeader(channel = channel)
            }

            items(
                items = podcasts,
                key = { it.id }
            ) { podcast ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.surface
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PodcastItem(
                        modifier = Modifier
                            .fillMaxWidth(),
                        podcast = podcast,
                        onClick = { onPodcastClick(podcast.id) }
                    )
                }
            }

            item(span = { GridItemSpan(2) }) {
                ChannelFooter(channel = channel)
            }

            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(LocalDimensionTheme.current.playerBarHeight))
            }
        }
    }
}

@Composable
private fun ChannelHeader(
    modifier: Modifier = Modifier,
    channel: Channel,
) {
    EpisodiveGradientBackground(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.5f),
        gradientColors = GradientColors(
            bottom = MaterialTheme.colorScheme.surface,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
        ) {
            Text(
                text = channel.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            val subtitle = stringResource(R.string.feature_channel_subtitle_format)
                .format(channel.count)

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ChannelFooter(
    modifier: Modifier = Modifier,
    channel: Channel,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface),
    ) {
        SectionHeader(
            title = stringResource(R.string.feature_channel_introduction),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Text(
                text = channel.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            )

            val uriHandler = LocalUriHandler.current

            EpisodiveIconText(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri(channel.link) },
                icon = {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = EpisodiveIcons.WorldShare,
                        contentDescription = "Website",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.feature_channel_website),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                iconLead = false
            )
        }
    }
}

@DevicePreviews
@Composable
private fun ChannelScreenPreview() {
    EpisodiveTheme {
        ChannelScreen(
            channel = channelTestData,
            podcasts = podcastTestDataList,
            onBackClick = {},
            onPodcastClick = {},
        )
    }
}

@DevicePreviews
@Composable
private fun ChannelHeaderPreview() {
    EpisodiveTheme {
        ChannelHeader(
            channel = channelTestData
        )
    }
}

@DevicePreviews
@Composable
private fun ChannelFooterPreview() {
    EpisodiveTheme {
        ChannelFooter(
            channel = channelTestData
        )
    }
}