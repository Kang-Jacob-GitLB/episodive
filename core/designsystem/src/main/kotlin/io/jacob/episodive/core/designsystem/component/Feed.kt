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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Feed
import io.jacob.episodive.core.model.mapper.toFeedsFromTrending
import io.jacob.episodive.core.testing.model.trendingFeedTestDataList


@Composable
fun FeedsSection(
    modifier: Modifier = Modifier,
    title: String,
    feeds: List<Feed>,
    onFeedClick: (Feed) -> Unit,
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
fun FeedItem(
    modifier: Modifier = Modifier,
    feed: Feed,
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

@DevicePreviews
@Composable
private fun FeedsSectionPreview() {
    EpisodiveTheme {
        FeedsSection(
            title = "Feeds",
            feeds = trendingFeedTestDataList.toFeedsFromTrending(),
            onFeedClick = {}
        )
    }
}