package io.jacob.episodive.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.jacob.episodive.core.designsystem.R
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.mapper.toHumanReadable
import io.jacob.episodive.core.testing.model.podcastTestData

@Composable
fun PodcastsSection(
    modifier: Modifier = Modifier,
    title: String,
    podcasts: List<Podcast>,
    onMore: () -> Unit = {},
    onPodcastClick: (Podcast) -> Unit = {},
) {
    SectionHeader(
        modifier = modifier,
        title = title,
    ) {
        val lazyListState = rememberLazyListState()
        val flingBehavior = rememberSnapFlingBehavior(
            lazyListState = lazyListState,
            snapPosition = SnapPosition.Start,
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            state = lazyListState,
            flingBehavior = flingBehavior,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(
                count = podcasts.size,
                key = { podcasts[it].id }
            ) { index ->
                val podcast = podcasts[index]
                PodcastItem(
                    podcast = podcast,
                    onClick = { onPodcastClick(podcast) }
                )
            }
        }
    }
}

@Composable
fun PodcastsWithAuthorSection(
    modifier: Modifier = Modifier,
    title: String,
    podcasts: List<Podcast>,
    onPodcastClick: (Podcast) -> Unit = {},
) {
    SectionHeader(
        modifier = modifier,
        title = title,
    ) {
        val lazyListState = rememberLazyListState()
        val flingBehavior = rememberSnapFlingBehavior(
            lazyListState = lazyListState,
            snapPosition = SnapPosition.Start,
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            state = lazyListState,
            flingBehavior = flingBehavior,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(
                count = podcasts.size,
                key = { podcasts[it].id }
            ) { index ->
                val podcast = podcasts[index]
                PodcastWithAuthorItem(
                    podcast = podcast,
                    onClick = { onPodcastClick(podcast) }
                )
            }
        }
    }
}

@Composable
fun PodcastItem(
    modifier: Modifier = Modifier,
    podcast: Podcast,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .height(210.dp)
            .clickable { onClick() },
    ) {
        StateImage(
            modifier = Modifier
                .size(140.dp)
                .clip(MaterialTheme.shapes.extraLarge),
            imageUrl = podcast.image,
            contentDescription = podcast.title,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = podcast.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${podcast.episodeCount} ${stringResource(R.string.core_designsystem_episodes)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PodcastWithAuthorItem(
    modifier: Modifier = Modifier,
    podcast: Podcast,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .height(210.dp)
            .clickable { onClick() },
    ) {
        StateImage(
            modifier = Modifier
                .size(140.dp)
                .clip(MaterialTheme.shapes.extraLarge),
            imageUrl = podcast.image,
            contentDescription = podcast.title,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = podcast.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = podcast.ownerName.ifEmpty { podcast.author },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PodcastDetailItem(
    modifier: Modifier = Modifier,
    podcast: Podcast,
    onClick: () -> Unit = {},
    onToggleFollowed: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick),
    ) {
        StateImage(
            modifier = Modifier
                .size(96.dp)
                .clip(MaterialTheme.shapes.extraLarge),
            imageUrl = podcast.image,
            contentDescription = podcast.title,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f),
                    text = podcast.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                EpisodiveIconToggleButton(
                    modifier = Modifier
                        .size(34.dp),
                    shape = MaterialTheme.shapes.medium,
                    checked = podcast.isFollowed,
                    onCheckedChange = { onToggleFollowed() },
                    icon = {
                        Icon(
                            modifier = Modifier.size(14.dp),
                            imageVector = EpisodiveIcons.PersonAdd,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = podcast.title,
                        )
                    },
                    checkedIcon = {
                        Icon(
                            modifier = Modifier.size(14.dp),
                            imageVector = EpisodiveIcons.PersonRemove,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = podcast.title,
                        )
                    },
                )
            }

            FlowRow(
                modifier = Modifier,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                podcast.ownerName.ifEmpty { podcast.author }.let { owner ->
                    if (owner.isNotEmpty()) {
                        EpisodiveIconText(
                            icon = {
                                Icon(
                                    imageVector = EpisodiveIcons.Attribution,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp),
                                )
                            },
                            text = {
                                Text(
                                    text = podcast.ownerName.ifEmpty { podcast.author },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        )
                    }
                }

                EpisodiveIconText(
                    icon = {
                        Icon(
                            imageVector = EpisodiveIcons.PublishedWithChanges,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp),
                        )
                    },
                    text = {
                        Text(
                            text = (podcast.newestItemPublishTime
                                ?: podcast.lastUpdateTime).toHumanReadable(),
                            maxLines = 1,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                )

                EpisodiveIconText(
                    icon = {
                        Icon(
                            imageVector = EpisodiveIcons.FormatListNumbered,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp),
                        )
                    },
                    text = {
                        Text(
                            text = "${podcast.episodeCount} ${stringResource(R.string.core_designsystem_episodes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HtmlTextContainer(
                text = podcast.description,
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
}

@Composable
fun PodcastSimpleItem(
    modifier: Modifier = Modifier,
    podcast: Podcast,
    onClick: () -> Unit,
    onToggleFollowed: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StateImage(
            modifier = Modifier
                .size(50.dp)
                .clip(MaterialTheme.shapes.medium),
            imageUrl = podcast.image,
            contentDescription = podcast.title,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = podcast.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = podcast.ownerName.ifEmpty { podcast.author },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        EpisodiveOutlinedButton(
            onClick = onToggleFollowed,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (podcast.isFollowed) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
            ),
        ) {
            Text(
                text = stringResource(
                    if (podcast.isFollowed) R.string.core_designsystem_unfollow
                    else R.string.core_designsystem_follow
                ),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@DevicePreviews
@Composable
private fun PodcastItemPreview() {
    EpisodiveTheme {
        PodcastItem(
            podcast = podcastTestData,
        )
    }
}

@DevicePreviews
@Composable
private fun PodcastWithAuthorPreview() {
    EpisodiveTheme {
        PodcastWithAuthorItem(
            podcast = podcastTestData,
        )
    }
}

@DevicePreviews
@Composable
private fun PodcastDetailItemPreview() {
    EpisodiveTheme {
        PodcastDetailItem(
            podcast = podcastTestData,
        )
    }
}

@DevicePreviews
@Composable
private fun PodcastSimpleItemPreview() {
    EpisodiveTheme {
        PodcastSimpleItem(
            podcast = podcastTestData,
            onClick = {},
            onToggleFollowed = {},
        )
    }
}