package io.jacob.episodive.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.jacob.episodive.core.designsystem.R
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.testing.model.podcastTestDataList

@Composable
fun PodcastsSection(
    modifier: Modifier = Modifier,
    title: String,
    podcasts: List<Podcast>,
    onMore: () -> Unit = {},
    onPodcastClick: (Podcast) -> Unit,
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
fun PodcastItem(
    modifier: Modifier = Modifier,
    podcast: Podcast,
    onClick: () -> Unit,
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

@DevicePreviews
@Composable
private fun PodcastsSectionPreview() {
    EpisodiveTheme {
        PodcastsSection(
            title = "Podcasts",
            podcasts = podcastTestDataList,
            onMore = {},
            onPodcastClick = {},
        )
    }
}