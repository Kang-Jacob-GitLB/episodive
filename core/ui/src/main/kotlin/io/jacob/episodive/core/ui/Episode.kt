package io.jacob.episodive.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import io.jacob.episodive.core.designsystem.component.ClipAnimationIconText
import io.jacob.episodive.core.designsystem.component.EpisodiveIconProgressButton
import io.jacob.episodive.core.designsystem.component.EpisodiveIconToggleButton
import io.jacob.episodive.core.designsystem.component.HtmlTextContainer
import io.jacob.episodive.core.designsystem.component.SectionHeader
import io.jacob.episodive.core.designsystem.component.StateImage
import io.jacob.episodive.core.designsystem.component.SubSectionHeader
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.mapper.toHumanReadable
import io.jacob.episodive.core.model.mapper.toIntSeconds
import io.jacob.episodive.core.model.mapper.toRelativeDate
import io.jacob.episodive.core.testing.model.episodeTestData
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Composable
fun EpisodesSection(
    modifier: Modifier = Modifier,
    title: String,
    episodes: List<Episode>,
    onEpisodeClick: (Episode) -> Unit,
    onToggleLikedEpisode: (Episode) -> Unit,
) {
    SectionHeader(
        modifier = modifier,
        title = title,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            episodes.forEach { episode ->
                EpisodeItem(
                    episode = episode,
                    progress = 0f,
                    isLoading = false,
                    onClick = { onEpisodeClick(episode) },
                    onToggleLiked = { onToggleLikedEpisode(episode) }
                )
            }
        }
    }
}

fun LazyListScope.episodeItems(
    itemModifier: Modifier = Modifier,
    episodes: List<Episode>,
    playingIndex: Int,
    onEpisodeClick: (Episode) -> Unit,
    onToggleLikedEpisode: (Episode) -> Unit,
) {
    itemsIndexed(
        items = episodes,
        key = { _, episode ->
            episode.id
        }
    ) { index, episode ->
        EpisodeItem(
            modifier = itemModifier,
            episode = episode,
            progress = 0f,
            isLoading = index == playingIndex,
            onClick = { onEpisodeClick(episode) },
            onToggleLiked = { onToggleLikedEpisode(episode) }
        )
    }
}

@Composable
fun EpisodeItem(
    modifier: Modifier = Modifier,
    episode: Episode,
    progress: Float = 0f,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    onToggleLiked: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StateImage(
            modifier = Modifier
                .size(72.dp)
                .clip(MaterialTheme.shapes.largeIncreased),
            imageUrl = episode.image.ifEmpty { episode.feedImage },
            contentDescription = episode.title,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            val subTitle = "%s • %s".format(
                episode.datePublished.toHumanReadable(),
                episode.feedTitle ?: episode.duration?.toHumanReadable() ?: ""
            ).trim()

            Text(
                text = subTitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            EpisodiveIconProgressButton(
                modifier = Modifier.size(32.dp),
                onClick = onClick,
                isLoading = isLoading,
                progress = progress,
                icon = {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = EpisodiveIcons.Play,
                        contentDescription = "Like",
                    )
                },
            )

            EpisodiveIconToggleButton(
                modifier = Modifier.size(32.dp),
                checked = episode.isLiked,
                onCheckedChange = { onToggleLiked() },
                colors = IconButtonDefaults.iconToggleButtonColors(
                    checkedContainerColor = Color.Transparent,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                icon = {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = EpisodiveIcons.Like,
                        contentDescription = "Like",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                checkedIcon = {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = EpisodiveIcons.LikeFilled,
                        contentDescription = "Unlike",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }
    }
}

@Stable
@Composable
fun PlayingEpisodesSection(
    modifier: Modifier = Modifier,
    playingEpisodes: List<Episode>,
    onEpisodeClick: (Episode) -> Unit,
) {
    SubSectionHeader(
        modifier = modifier,
        title = stringResource(R.string.core_ui_continue),
    ) {
        val lazyListState = rememberLazyListState()
        val flingBehavior = rememberSnapFlingBehavior(
            lazyListState = lazyListState,
            snapPosition = SnapPosition.Start,
        )

        val firstEpisodeId = playingEpisodes.firstOrNull()?.id

        LaunchedEffect(firstEpisodeId) {
            if (firstEpisodeId != null) {
                lazyListState.animateScrollToItem(0)
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            state = lazyListState,
            flingBehavior = flingBehavior,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            playingEpisodes(
                playingEpisodes = playingEpisodes,
                onEpisodeClick = onEpisodeClick
            )
        }
    }
}

fun LazyListScope.playingEpisodes(
    itemModifier: Modifier = Modifier,
    playingEpisodes: LazyPagingItems<Episode>,
    onEpisodeClick: (Episode) -> Unit,
) = items(
    count = playingEpisodes.itemCount,
    key = { playingEpisodes[it]?.id ?: it },
    itemContent = { index ->
        playingEpisodes[index]?.let { playedEpisode ->
            PlayingEpisodeItem(
                modifier = itemModifier.animateItem(),
                playedEpisode = playedEpisode,
                onClick = { onEpisodeClick(playedEpisode) }
            )
        }
    }
)

fun LazyListScope.playingEpisodes(
    itemModifier: Modifier = Modifier,
    playingEpisodes: List<Episode>,
    onEpisodeClick: (Episode) -> Unit,
) = items(
    items = playingEpisodes,
    key = { it.id },
    itemContent = { playedEpisode ->
        PlayingEpisodeItem(
            modifier = itemModifier.animateItem(),
            playedEpisode = playedEpisode,
            onClick = { onEpisodeClick(playedEpisode) }
        )
    }
)

@Composable
fun PlayingEpisodeItem(
    modifier: Modifier = Modifier,
    playedEpisode: Episode,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .size(width = 192.dp, height = 84.dp),
        shape = MaterialTheme.shapes.largeIncreased,
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
                    .clip(MaterialTheme.shapes.largeIncreased),
                imageUrl = playedEpisode.image.ifEmpty { playedEpisode.feedImage },
                contentDescription = playedEpisode.title,
            )

            Column(
                modifier = Modifier
                    .width(98.dp)
            ) {
                Text(
                    text = playedEpisode.playedAt?.toRelativeDate() ?: playedEpisode.feedTitle
                    ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = playedEpisode.title,
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
                        val duration = playedEpisode.duration?.toIntSeconds()
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
fun PlayedEpisodeItem(
    modifier: Modifier = Modifier,
    playedEpisode: Episode,
    showMoreInfo: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable { onClick() },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StateImage(
            modifier = Modifier
                .size(68.dp)
                .clip(MaterialTheme.shapes.largeIncreased),
            imageUrl = playedEpisode.image.ifEmpty { playedEpisode.feedImage },
            contentDescription = playedEpisode.title,
        )

        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = playedEpisode.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape),
                    color = if (playedEpisode.isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    gapSize = (-4).dp,
                    drawStopIndicator = {},
                    progress = { playedEpisode.progress },
                )

                Text(
                    text = "${(playedEpisode.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (showMoreInfo) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text =
                        if (playedEpisode.isCompleted) stringResource(R.string.core_ui_completed)
                        else if (playedEpisode.remain != null) "${playedEpisode.remain?.toHumanReadable()} ${
                            stringResource(
                                R.string.core_ui_left
                            )
                        }"
                        else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun EpisodeDetailItem(
    modifier: Modifier = Modifier,
    episode: Episode,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        StateImage(
            modifier = Modifier
                .size(200.dp)
                .clip(MaterialTheme.shapes.extraLarge),
            imageUrl = episode.image.ifEmpty { episode.feedImage },
            contentDescription = episode.title,
        )

        Spacer(modifier = Modifier.height(8.dp))

        val subTitle = "%s • %s".format(
            episode.datePublished.toHumanReadable(),
            episode.duration?.toHumanReadable() ?: episode.feedTitle
        ).trim()

        Text(
            text = subTitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = episode.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        HtmlTextContainer(
            text = episode.description ?: "",
            enableLinks = false,
        ) {
            Text(
                text = it,
                maxLines = 4,
                minLines = 4,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun EpisodeClipItem(
    modifier: Modifier = Modifier,
    episode: Episode,
    isPlaying: Boolean,
    remaining: Duration,
    onClick: () -> Unit,
    onPlayEpisode: () -> Unit,
    onToggleLikedEpisode: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.largeIncreased,
        onClick = onClick,
        color = Color.Transparent,
    ) {
        StateImage(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 20.dp),
            imageUrl = episode.image.ifEmpty { episode.feedImage },
            contentDescription = episode.title,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.6f)
                .background(MaterialTheme.colorScheme.background)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        ) {
            Card(
                modifier = Modifier
                    .size(250.dp),
                shape = MaterialTheme.shapes.largeIncreased,
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
            ) {
                StateImage(
                    modifier = Modifier
                        .fillMaxSize(),
                    imageUrl = episode.image.ifEmpty { episode.feedImage },
                    contentDescription = episode.title,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = episode.title,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                HtmlTextContainer(
                    text = episode.description ?: "",
                    enableLinks = false,
                ) {
                    Text(
                        text = it,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClipAnimationIconText(
                    text = remaining.toHumanReadable(),
                    isPlaying = isPlaying,
                )

                Spacer(modifier = Modifier.weight(1f))

                EpisodiveIconToggleButton(
                    checked = episode.isLiked,
                    onCheckedChange = { onToggleLikedEpisode() },
                    colors = IconButtonDefaults.iconToggleButtonColors(
                        checkedContainerColor = Color.Transparent,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    icon = {
                        Icon(
                            imageVector = EpisodiveIcons.Like,
                            contentDescription = "Like",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    checkedIcon = {
                        Icon(
                            imageVector = EpisodiveIcons.LikeFilled,
                            contentDescription = "Unlike",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )

                EpisodiveIconToggleButton(
                    checked = isPlaying,
                    onCheckedChange = { onPlayEpisode() },
                    colors = IconButtonDefaults.iconToggleButtonColors(
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    icon = {
                        Icon(
                            imageVector = EpisodiveIcons.Play,
                            contentDescription = "Play",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    checkedIcon = {
                        Icon(
                            imageVector = EpisodiveIcons.Pause,
                            contentDescription = "Pause",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }
        }
    }
}

@DevicePreviews
@Composable
private fun EpisodeItemPreview() {
    EpisodiveTheme {
        EpisodeItem(
            episode = episodeTestData.copy(likedAt = Instant.fromEpochSeconds(1234)),
            progress = 0f,
            isLoading = false,
            onClick = {},
            onToggleLiked = {},
        )
    }
}

@DevicePreviews
@Composable
private fun PlayingEpisodesPreview() {
    EpisodiveTheme {
        PlayingEpisodeItem(
            playedEpisode = episodeTestData,
            onClick = {},
        )
    }
}

@DevicePreviews
@Composable
private fun PlayedEpisodesPreview() {
    EpisodiveTheme {
        PlayedEpisodeItem(
            playedEpisode = episodeTestData,
            onClick = {},
        )
    }
}

@DevicePreviews
@Composable
private fun EpisodeDetailItemPreview() {
    EpisodiveTheme {
        EpisodeDetailItem(
            episode = episodeTestData,
            onClick = {},
        )
    }
}

@DevicePreviews
@Composable
private fun EpisodeClipItemPreview() {
    EpisodiveTheme {
        EpisodeClipItem(
            episode = episodeTestData.copy(
                clipStartTime = Instant.fromEpochSeconds(30),
                clipDuration = 60.seconds,
            ),
            isPlaying = true,
            remaining = 45.seconds,
            onClick = {},
            onPlayEpisode = {},
            onToggleLikedEpisode = {},
        )
    }
}