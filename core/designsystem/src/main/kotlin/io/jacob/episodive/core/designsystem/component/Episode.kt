package io.jacob.episodive.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.PlayedEpisode
import io.jacob.episodive.core.model.mapper.toDurationSeconds
import io.jacob.episodive.core.model.mapper.toHumanReadable
import io.jacob.episodive.core.model.mapper.toIntSeconds
import io.jacob.episodive.core.testing.model.episodeTestDataList
import kotlin.time.Clock

@Composable
fun EpisodesSection(
    modifier: Modifier = Modifier,
    title: String,
    episodes: List<Episode>,
    onEpisodeClick: (Episode) -> Unit,
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
                    onClick = { onEpisodeClick(episode) }
                )
            }
        }
    }
}

@Composable
fun EpisodeItem(
    modifier: Modifier = Modifier,
    episode: Episode,
    onClick: () -> Unit,
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
                .clip(RoundedCornerShape(16.dp)),
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
                episode.duration?.toHumanReadable() ?: episode.feedTitle
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
            Icon(
                imageVector = EpisodiveIcons.PlayArrow,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = "Play episode",
            )
        }
    }
}

@Composable
fun PlayingEpisodesSection(
    modifier: Modifier = Modifier,
    playingEpisodes: List<PlayedEpisode>,
    onEpisodeClick: (PlayedEpisode) -> Unit,
) {
    SubSectionHeader(
        modifier = modifier,
        title = stringResource(R.string.core_designsystem_continue),
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
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
fun PlayingEpisodeItem(
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

@DevicePreviews
@Composable
private fun EpisodeSectionPreview() {
    EpisodiveTheme {
        EpisodesSection(
            title = "Episodes",
            episodes = episodeTestDataList,
            onEpisodeClick = {}
        )
    }
}

@DevicePreviews
@Composable
private fun PlayingEpisodesSectionPreview() {
    EpisodiveTheme {
        PlayingEpisodesSection(
            playingEpisodes = episodeTestDataList.map {
                PlayedEpisode(
                    episode = it,
                    playedAt = Clock.System.now(),
                    position = (it.duration?.toIntSeconds()?.let { seconds -> seconds / 2 }
                        ?: 0).toDurationSeconds(),
                    isCompleted = false,
                )
            },
            onEpisodeClick = {}
        )
    }
}