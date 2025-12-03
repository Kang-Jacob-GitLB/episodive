package io.jacob.episodive.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.jacob.episodive.core.designsystem.component.DominantRegion
import io.jacob.episodive.core.designsystem.component.SectionHeader
import io.jacob.episodive.core.designsystem.component.StateImage
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.testing.model.channelTestData

@Composable
fun ChannelSection(
    modifier: Modifier = Modifier,
    title: String,
    channels: List<Channel>,
    onChannelClick: (Channel) -> Unit,
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
                items = channels,
                key = { it.id }
            ) { channel ->
                ChannelItem(
                    channel = channel,
                    onClick = { onChannelClick(channel) }
                )
            }
        }
    }
}

@Composable
fun ChannelItem(
    modifier: Modifier = Modifier,
    channel: Channel,
    onClick: () -> Unit,
) {
    var backgroundColor by remember { mutableStateOf(Color.Cyan) }

    Column(
        modifier = modifier
            .width(250.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable { onClick() },
    ) {
        StateImage(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            imageUrl = channel.image,
            contentDescription = channel.title,
            onDominantColorExtracted = {
                backgroundColor = it
            },
            dominantRegion = DominantRegion.Bottom
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(color = backgroundColor.copy(alpha = .5f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                modifier = Modifier
                    .padding(8.dp),
                text = channel.description,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@DevicePreviews
@Composable
private fun ChannelItemPreview() {
    EpisodiveTheme {
        ChannelItem(
            channel = channelTestData,
            onClick = {}
        )
    }
}