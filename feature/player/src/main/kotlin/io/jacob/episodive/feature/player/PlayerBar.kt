package io.jacob.episodive.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.jacob.episodive.core.designsystem.component.EpisodiveIconToggleButton
import io.jacob.episodive.core.designsystem.component.EpisodiveSeeker
import io.jacob.episodive.core.designsystem.component.StateImage
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Chapter
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.podcastTestData
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration.Companion.seconds

@Composable
fun PlayerBar(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
    onPodcastClick: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var isShowPlayer by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is PlayerEffect.NavigateToPodcast -> {}
                is PlayerEffect.ShowPlayerBottomSheet -> {
                    isShowPlayer = true
                }

                is PlayerEffect.HidePlayerBottomSheet -> {
                    isShowPlayer = false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = state is PlayerState.Success,
            modifier = Modifier.align(Alignment.BottomCenter), // 위치 유지
            enter = slideInVertically(
                initialOffsetY = { it }, // 자기 키만큼 아래에서 등장
                animationSpec = tween(300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            )
        ) {
            val s = state as? PlayerState.Success ?: return@AnimatedVisibility

            PlayerBar(
                modifier = modifier,
                podcast = s.podcast,
                nowPlaying = s.nowPlaying,
                progress = s.progress,
                isPlaying = s.isPlaying,
                isLike = s.isLiked,
                chapters = s.chapters,
                dominantColor = Color(s.dominantColor),
                onExpand = { viewModel.sendAction(PlayerAction.ExpandPlayer) },
                onToggleLike = { viewModel.sendAction(PlayerAction.ToggleLike) },
                onPlayOrPause = { viewModel.sendAction(PlayerAction.PlayOrPause) },
            )
        }
    }

    if (isShowPlayer) {
        PlayerBottomSheet(onPodcastClick = onPodcastClick)
    }
}

@Composable
private fun PlayerBar(
    modifier: Modifier = Modifier,
    podcast: Podcast,
    nowPlaying: Episode,
    progress: Progress,
    isPlaying: Boolean,
    isLike: Boolean,
    chapters: List<Chapter>,
    dominantColor: Color = MaterialTheme.colorScheme.primaryContainer,
    onExpand: () -> Unit,
    onToggleLike: () -> Unit,
    onPlayOrPause: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(LocalDimensionTheme.current.playerBarHeight)
            .padding(horizontal = 6.dp)
            .padding(bottom = 6.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = dominantColor,
        ),
        elevation = CardDefaults.cardElevation(4.dp),
        onClick = onExpand
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StateImage(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(MaterialTheme.shapes.medium),
                    imageUrl = nowPlaying.image.ifEmpty { nowPlaying.feedImage },
                    contentDescription = nowPlaying.title
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(),
                        text = nowPlaying.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(),
                        text = podcast.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                EpisodiveIconToggleButton(
                    checked = isLike,
                    onCheckedChange = { onToggleLike() },
                    colors = IconButtonDefaults.iconToggleButtonColors(
                        checkedContainerColor = Color.Transparent,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    icon = {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = EpisodiveIcons.Like,
                            contentDescription = "Like",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    checkedIcon = {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = EpisodiveIcons.LikeFilled,
                            contentDescription = "Unlike",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )

                EpisodiveIconToggleButton(
                    checked = isPlaying,
                    onCheckedChange = { onPlayOrPause() },
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

            EpisodiveSeeker(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 6.dp),
                progress = progress,
                onSeekTo = {},
                chapters = chapters,
                onChapterName = {},
                isControllable = false,
            )
        }
    }
}

@DevicePreviews
@Composable
private fun PlayerBarPreview() {
    EpisodiveTheme {
        PlayerBar(
            podcast = podcastTestData,
            nowPlaying = episodeTestData,
            progress = Progress(
                position = 30.seconds,
                buffered = 60.seconds,
                duration = 100.seconds,
            ),
            isPlaying = false,
            isLike = false,
            chapters = listOf(
                Chapter("Chapter 1", 0.seconds, 10.seconds),
                Chapter("Chapter 2", 10.seconds, 80.seconds),
                Chapter("Chapter 3", 80.seconds, 100.seconds),
            ),
            onExpand = {},
            onToggleLike = {},
            onPlayOrPause = {},
        )
    }
}