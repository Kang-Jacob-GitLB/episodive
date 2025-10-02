package io.jacob.episodive.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import io.jacob.episodive.core.designsystem.tooling.ThemePreviews
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.testing.model.podcastTestData

@Composable
fun PodcastItem(
    modifier: Modifier = Modifier,
    podcast: Podcast,
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

@ThemePreviews
@Composable
private fun PodcastItemPreview() {
    EpisodiveTheme {
        PodcastItem(
            podcast = podcastTestData,
            onClick = {},
        )
    }
}