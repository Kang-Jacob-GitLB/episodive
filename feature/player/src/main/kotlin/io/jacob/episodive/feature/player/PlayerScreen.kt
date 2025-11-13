package io.jacob.episodive.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import io.jacob.episodive.core.designsystem.component.EpisodiveButton
import io.jacob.episodive.core.designsystem.component.EpisodiveCenterTopAppBar
import io.jacob.episodive.core.designsystem.component.EpisodiveDial
import io.jacob.episodive.core.designsystem.component.EpisodiveDragHandle
import io.jacob.episodive.core.designsystem.component.EpisodiveGradientBackground
import io.jacob.episodive.core.designsystem.component.EpisodiveIconButton
import io.jacob.episodive.core.designsystem.component.EpisodiveIconToggleButton
import io.jacob.episodive.core.designsystem.component.EpisodiveTextButton
import io.jacob.episodive.core.designsystem.component.HtmlTextContainer
import io.jacob.episodive.core.designsystem.component.SectionHeader
import io.jacob.episodive.core.designsystem.component.StateImage
import io.jacob.episodive.core.designsystem.component.episodeItems
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.GradientColors
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.model.mapper.toLongMillis
import io.jacob.episodive.core.model.mapper.toMediaTime
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestData
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import kotlin.time.Duration.Companion.seconds

@Composable
fun PlayerBottomSheet(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
    onPodcastClick: (Long) -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is PlayerEffect.NavigateToPodcast -> {}
                is PlayerEffect.ShowPlayerBottomSheet -> {}
                is PlayerEffect.HidePlayerBottomSheet -> sheetState.hide()
            }
        }
    }

    val s = state as? PlayerState.Success ?: return

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = { viewModel.sendAction(PlayerAction.CollapsePlayer) },
        sheetState = sheetState,
        dragHandle = null,
        scrimColor = Color.Transparent,
        contentWindowInsets = { WindowInsets(0) },
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = true,
        ),
    ) {
        fun collapse() {
            scope.launch {
                sheetState.hide()
                viewModel.sendAction(PlayerAction.CollapsePlayer)
            }
        }

        PlayerScreen(
            modifier = Modifier,
            podcast = s.podcast,
            nowPlaying = s.nowPlaying,
            progress = s.progress,
            isPlaying = s.isPlaying,
            isLike = s.isLiked,
            dominantColor = Color(s.dominantColor),
            onCollapse = { collapse() },
            onToggleLike = { viewModel.sendAction(PlayerAction.ToggleLike) },
            onSeekTo = { viewModel.sendAction(PlayerAction.SeekTo(it)) },
            onPlayOrPause = { viewModel.sendAction(PlayerAction.PlayOrPause) },
            onBackward = { viewModel.sendAction(PlayerAction.SeekBackward) },
            onForward = { viewModel.sendAction(PlayerAction.SeekForward) },
            onPrevious = { viewModel.sendAction(PlayerAction.Previous) },
            onNext = { viewModel.sendAction(PlayerAction.Next) },
            onPodcastClick = { podcastId ->
                onPodcastClick(podcastId)
                collapse()
            },
            playlist = s.playlist,
            indexOfList = s.indexOfList,
            onEpisodeClick = { viewModel.sendAction(PlayerAction.ClickEpisode(it)) },
            onPlayIndex = { viewModel.sendAction(PlayerAction.PlayIndex(it)) },
            onToggleEpisodeLiked = { viewModel.sendAction(PlayerAction.ToggleEpisodeLiked(it)) },
            speed = s.speed,
            onSpeedChange = { viewModel.sendAction(PlayerAction.Speed(it)) },
        )
    }
}


@Composable
private fun PlayerScreen(
    modifier: Modifier = Modifier,
    podcast: Podcast,
    nowPlaying: Episode,
    progress: Progress,
    isPlaying: Boolean,
    isLike: Boolean,
    dominantColor: Color = MaterialTheme.colorScheme.primaryContainer,
    onCollapse: () -> Unit = {},
    onToggleLike: () -> Unit = {},
    onSeekTo: (Long) -> Unit = {},
    onPlayOrPause: () -> Unit = {},
    onBackward: () -> Unit = {},
    onForward: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onPodcastClick: (Long) -> Unit = {},
    playlist: List<Episode>,
    indexOfList: Int,
    onEpisodeClick: (Episode) -> Unit = {},
    onPlayIndex: (Int) -> Unit = {},
    onToggleEpisodeLiked: (Episode) -> Unit = {},
    speed: Float,
    onSpeedChange: (Float) -> Unit = {},
) {
    val listState = rememberLazyListState()
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        state = listState,
    ) {
        item {
            EpisodiveGradientBackground(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillParentMaxHeight(0.92f),
                gradientColors = GradientColors(
                    top = dominantColor,
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = systemBarsPadding.calculateTopPadding()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    EpisodiveCenterTopAppBar(
                        title = {},
                        navigationIcon = EpisodiveIcons.KeyboardArrowDown,
                        navigationIconContentDescription = "Down",
                        actionIcon = if (isLike) EpisodiveIcons.Favorite else EpisodiveIcons.FavoriteBorder,
                        actionIconContentDescription = "Like",
                        onNavigationClick = onCollapse,
                        onActionClick = onToggleLike,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        windowInsets = WindowInsets(0),
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    StateImage(
                        modifier = Modifier
                            .size(300.dp)
                            .clip(MaterialTheme.shapes.extraExtraLarge),
                        imageUrl = nowPlaying.image.ifEmpty { nowPlaying.feedImage },
                        contentDescription = nowPlaying.title
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            modifier = Modifier
                                .clickable { onPodcastClick(podcast.id) },
                            text = podcast.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )

                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(),
                            text = nowPlaying.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }

                    ControlPanelProgress(
                        isPlaying = isPlaying,
                        progress = progress,
                        onSeekTo = onSeekTo
                    )

                    ControlPanelBottom(
                        isPlaying = isPlaying,
                        onPlayOrPause = onPlayOrPause,
                        onBackward = onBackward,
                        onForward = onForward,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onSpeed = { showSpeedSheet = true },
                        speed = speed,
                        onList = { showPlaylistSheet = true },
                    )

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            InfoSection(
                episode = nowPlaying
            )
        }

        item {
            Spacer(modifier = Modifier.height(50.dp))
        }
    }

    if (showSpeedSheet) {
        SpeedSheet(
            speed = speed,
            onSpeedChange = onSpeedChange,
            onDismiss = { showSpeedSheet = false }
        )
    }

    if (showPlaylistSheet) {
        PlaylistSheet(
            playlist = playlist,
            playingIndex = indexOfList,
            onEpisodeClick = onEpisodeClick,
            onToggleEpisodeLiked = onToggleEpisodeLiked,
            onDismiss = { showPlaylistSheet = false }
        )
    }
}

@Composable
private fun ControlPanelProgress(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    progress: Progress,
    onSeekTo: (Long) -> Unit = {},
) {
    var position by remember { mutableFloatStateOf(progress.positionRatio) }

    LaunchedEffect(progress.position) {
        position = progress.positionRatio
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier,
        ) {
            Slider(
                value = position,
                onValueChange = { value ->
                    position = value
                },
                onValueChangeFinished = {
                    onSeekTo((position * progress.duration.toLongMillis()).toLong())
                },
                thumb = { sliderState ->
                    val size = if (sliderState.isDragging) 24.dp else 16.dp
                    Box(
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                },
                track = { sliderState ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(progress.bufferedRatio)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(sliderState.value)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = progress.position.toMediaTime(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = progress.duration.toMediaTime(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ControlPanelBottom(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    onPlayOrPause: () -> Unit = {},
    onBackward: () -> Unit = {},
    onForward: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onSpeed: () -> Unit = {},
    speed: Float,
    onList: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 38.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EpisodiveIconButton(
                modifier = Modifier.size(56.dp),
                onClick = onBackward,
                icon = {
                    Icon(
                        modifier = Modifier.size(40.dp),
                        imageVector = EpisodiveIcons.Replay10,
                        contentDescription = "Replay",
                    )
                }
            )

            EpisodiveIconButton(
                modifier = Modifier.size(56.dp),
                onClick = onPrevious,
                icon = {
                    Icon(
                        modifier = Modifier.size(48.dp),
                        imageVector = EpisodiveIcons.SkipPrevious,
                        contentDescription = "Previous",
                    )
                }
            )

            EpisodiveIconToggleButton(
                modifier = Modifier.size(56.dp),
                checked = isPlaying,
                onCheckedChange = { onPlayOrPause() },
                icon = {
                    Icon(
                        modifier = Modifier.size(40.dp),
                        imageVector = EpisodiveIcons.PlayArrow,
                        contentDescription = "Play",
                    )
                },
                checkedIcon = {
                    Icon(
                        modifier = Modifier.size(40.dp),
                        imageVector = EpisodiveIcons.Pause,
                        contentDescription = "Pause",
                    )
                },
                colors = IconButtonDefaults.iconToggleButtonColors(
                    checkedContainerColor = MaterialTheme.colorScheme.onBackground,
                    checkedContentColor = MaterialTheme.colorScheme.background,
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                )
            )

            EpisodiveIconButton(
                modifier = Modifier.size(56.dp),
                onClick = onNext,
                icon = {
                    Icon(
                        modifier = Modifier.size(48.dp),
                        imageVector = EpisodiveIcons.SkipNext,
                        contentDescription = "Next",
                    )
                }
            )

            EpisodiveIconButton(
                modifier = Modifier.size(56.dp),
                onClick = onForward,
                icon = {
                    Icon(
                        modifier = Modifier.size(40.dp),
                        imageVector = EpisodiveIcons.Forward30,
                        contentDescription = "Forward",
                    )
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val decimalFormat = DecimalFormat("#.#")

            EpisodiveTextButton(
                modifier = Modifier.size(56.dp),
                onClick = onSpeed,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    style = MaterialTheme.typography.titleLarge,
                    text = "${decimalFormat.format(speed)}x"
                )
            }

            EpisodiveIconButton(
                modifier = Modifier.size(56.dp),
                onClick = onList,
                icon = {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        imageVector = EpisodiveIcons.List,
                        contentDescription = "List",
                    )
                }
            )
        }
    }
}

@Composable
private fun InfoSection(
    modifier: Modifier = Modifier,
    episode: Episode,
) {
    Card(
        modifier = modifier.padding(16.dp),
    ) {
        SectionHeader(
            modifier = Modifier.padding(vertical = 16.dp),
            title = stringResource(R.string.feature_player_more_info),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            HtmlTextContainer(text = episode.description ?: "") {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PlaylistSheet(
    modifier: Modifier = Modifier,
    playlist: List<Episode>,
    playingIndex: Int,
    onEpisodeClick: (Episode) -> Unit = {},
    onToggleEpisodeLiked: (Episode) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { EpisodiveDragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            episodeItems(
                episodes = playlist,
                playingIndex = playingIndex,
                onEpisodeClick = onEpisodeClick,
                onToggleEpisodeLiked = onToggleEpisodeLiked
            )
        }
    }
}

@Composable
private fun SpeedSheet(
    modifier: Modifier = Modifier,
    speed: Float,
    onSpeedChange: (Float) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val decimalFormat = DecimalFormat("#.#")
    val manualSpeed = listOf(0.5f, 1f, 1.5f, 2f, 3.5f)
    val isDefaultSpeed = speed == 1f

    ModalBottomSheet(
        modifier = modifier
            .fillMaxWidth(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { EpisodiveDragHandle() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${decimalFormat.format(speed)}배속",
                style = MaterialTheme.typography.headlineMedium,
                color = if (isDefaultSpeed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            )

            EpisodiveDial(
                value = speed,
                onValueChange = onSpeedChange,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                manualSpeed.forEach { speed ->
                    EpisodiveButton(
                        modifier = Modifier
                            .size(48.dp),
                        onClick = { onSpeedChange(speed) },
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        buttonColors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    ) {
                        Text(
                            text = decimalFormat.format(speed),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

    }
}

@DevicePreviews
@Composable
private fun PlayerScreenPreview() {
    EpisodiveTheme {
        PlayerScreen(
            podcast = podcastTestData,
            nowPlaying = episodeTestData,
            progress = Progress(1000.seconds, 2000.seconds, 3000.seconds),
            isPlaying = true,
            isLike = false,
            playlist = episodeTestDataList,
            indexOfList = 0,
            speed = 1f,
        )
    }
}

@DevicePreviews
@Composable
private fun InfoSectionPreview() {
    EpisodiveTheme {
        InfoSection(episode = episodeTestData)
    }
}

@DevicePreviews
@Composable
private fun PlaylistSheetPreview() {
    EpisodiveTheme {
        PlaylistSheet(
            playlist = episodeTestDataList,
            playingIndex = 0,
        )
    }
}

@DevicePreviews
@Composable
private fun SpeedSheetPreview() {
    EpisodiveTheme {
        SpeedSheet(
            speed = 1f,
        )
    }
}