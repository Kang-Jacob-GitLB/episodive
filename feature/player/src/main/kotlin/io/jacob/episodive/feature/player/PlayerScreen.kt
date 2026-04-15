package io.jacob.episodive.feature.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.jacob.episodive.core.designsystem.component.DominantRegion
import io.jacob.episodive.core.designsystem.component.EpisodiveButton
import io.jacob.episodive.core.designsystem.component.EpisodiveCenterTopAppBar
import io.jacob.episodive.core.designsystem.component.EpisodiveDial
import io.jacob.episodive.core.designsystem.component.EpisodiveDragHandle
import io.jacob.episodive.core.designsystem.component.EpisodiveGradientBackground
import io.jacob.episodive.core.designsystem.component.EpisodiveIconButton
import io.jacob.episodive.core.designsystem.component.EpisodiveIconProgressButton
import io.jacob.episodive.core.designsystem.component.EpisodiveSwipeDismissSnackbarHost
import io.jacob.episodive.core.designsystem.component.EpisodiveIconToggleButton
import io.jacob.episodive.core.designsystem.component.EpisodiveSeeker
import io.jacob.episodive.core.designsystem.component.EpisodiveTextButton
import io.jacob.episodive.core.designsystem.component.EpisodiveViewToggleButton
import io.jacob.episodive.core.designsystem.component.FadingEdgeText
import io.jacob.episodive.core.designsystem.component.HtmlTextContainer
import io.jacob.episodive.core.designsystem.component.StateImage
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.GradientColors
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Chapter
import io.jacob.episodive.core.model.DownloadStatus
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.model.mapper.toHumanReadable
import io.jacob.episodive.core.model.mapper.toLongMillis
import io.jacob.episodive.core.model.mapper.toMediaTime
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.ui.R as uiR
import io.jacob.episodive.core.ui.ChapterItem
import io.jacob.episodive.core.ui.PodcastSimpleItem
import io.jacob.episodive.core.ui.episodeItems

import kotlinx.coroutines.launch
import java.text.DecimalFormat
import kotlin.time.Duration.Companion.seconds

@Composable
fun PlayerBottomSheet(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
    onPodcastClick: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val state by viewModel.state.collectAsStateWithLifecycle()

    val unsavedMessage = stringResource(uiR.string.core_ui_snackbar_unsaved)
    val undoLabel = stringResource(uiR.string.core_ui_snackbar_undo)
    val sleepTimerExpiredMessage = stringResource(R.string.feature_player_sleep_timer_expired)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PlayerEffect.NavigateToPodcast -> onPodcastClick(effect.podcastId)
                is PlayerEffect.ShowPlayerBottomSheet -> {}
                is PlayerEffect.HidePlayerBottomSheet -> sheetState.hide()
                is PlayerEffect.ShowUnsaveSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = unsavedMessage,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.sendAction(PlayerAction.ToggleSavedEpisode(effect.episode))
                    }
                }

                is PlayerEffect.SleepTimerExpired -> {
                    snackbarHostState.showSnackbar(
                        message = sleepTimerExpiredMessage,
                        duration = SnackbarDuration.Short,
                    )
                }
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

        Box {
        PlayerScreen(
            modifier = Modifier,
            podcast = s.podcast,
            nowPlaying = s.nowPlaying,
            progress = s.progress,
            isPlaying = s.isPlaying,
            onCollapse = { collapse() },
            onToggleLike = { viewModel.sendAction(PlayerAction.ToggleLike) },
            onToggleSave = { viewModel.sendAction(PlayerAction.ToggleSave) },
            onSeekTo = { viewModel.sendAction(PlayerAction.SeekTo(it)) },
            onPlayOrPause = { viewModel.sendAction(PlayerAction.PlayOrPause) },
            onBackward = { viewModel.sendAction(PlayerAction.SeekBackward) },
            onForward = { viewModel.sendAction(PlayerAction.SeekForward) },
            onPrevious = { viewModel.sendAction(PlayerAction.Previous) },
            onNext = { viewModel.sendAction(PlayerAction.Next) },
            onPodcastClick = {
                viewModel.sendAction(PlayerAction.ClickPodcast(it))
                collapse()
            },
            playlist = s.playlist,
            indexOfList = s.indexOfList,
            onEpisodeClick = { viewModel.sendAction(PlayerAction.ClickEpisode(it)) },
            onPlayIndex = { viewModel.sendAction(PlayerAction.PlayIndex(it)) },
            onToggleLikedEpisode = { viewModel.sendAction(PlayerAction.ToggleLikedEpisode(it)) },
            onToggleSavedEpisode = { viewModel.sendAction(PlayerAction.ToggleSavedEpisode(it)) },
            speed = s.speed,
            onSpeedChange = { viewModel.sendAction(PlayerAction.Speed(it)) },
            chapters = s.chapters,
            onToggleFollowedPodcast = { viewModel.sendAction(PlayerAction.ToggleFollowedPodcast(it)) },
            cue = s.cue,
            sleepTimerRemainingMs = s.sleepTimerRemainingMs,
            onSetSleepTimer = { viewModel.sendAction(PlayerAction.SetSleepTimer(it)) },
            onCancelSleepTimer = { viewModel.sendAction(PlayerAction.CancelSleepTimer) },
            onSleepTimerEndOfEpisode = { viewModel.sendAction(PlayerAction.SleepTimerEndOfEpisode) },
        )

            EpisodiveSwipeDismissSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}


@Composable
internal fun PlayerScreen(
    modifier: Modifier = Modifier,
    podcast: Podcast,
    nowPlaying: Episode,
    progress: Progress,
    isPlaying: Boolean,
    onCollapse: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPlayOrPause: () -> Unit,
    onBackward: () -> Unit,
    onForward: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPodcastClick: (Podcast) -> Unit,
    playlist: List<Episode>,
    indexOfList: Int,
    onEpisodeClick: (Episode) -> Unit,
    onPlayIndex: (Int) -> Unit,
    onToggleLikedEpisode: (Episode) -> Unit,
    onToggleSavedEpisode: (Episode) -> Unit = {},
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    chapters: List<Chapter>,
    onToggleFollowedPodcast: (Podcast) -> Unit,
    cue: String,
    sleepTimerRemainingMs: Long? = null,
    onSetSleepTimer: (Long) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    onSleepTimerEndOfEpisode: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var chapterIndex by remember { mutableStateOf(0) }
    var dominantColor by remember { mutableStateOf(Color.DarkGray) }


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
                        navigationIcon = EpisodiveIcons.CaretDown,
                        navigationIconContentDescription = "Down",
                        actionIcon = if (nowPlaying.isLiked) EpisodiveIcons.LikeFilled else EpisodiveIcons.Like,
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

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .aspectRatio(1f),
                    ) {
                        StateImage(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.extraExtraLarge),
                            size = 600,
                            imageUrl = nowPlaying.image.ifEmpty { nowPlaying.feedImage },
                            contentDescription = nowPlaying.title,
                            onDominantColorExtracted = { dominantColor = it },
                            dominantRegion = DominantRegion.Top,
                            brightnessAdjustment = -0.2f
                        )

                        PushUpCue(
                            modifier = Modifier
                                .align(Alignment.BottomCenter),
                            title = cue,
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FadingEdgeText(
                            modifier = Modifier
                                .clickable { onPodcastClick(podcast) },
                            text = podcast.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )

                        FadingEdgeText(
                            modifier = Modifier
                                .fillMaxWidth(),
                            text = nowPlaying.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }

                    ControlPanelProgress(
                        isPlaying = isPlaying,
                        progress = progress,
                        chapters = chapters,
                        onSeekTo = onSeekTo,
                        onChapterIndex = { chapterIndex = it }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    ControlPanelBottom(
                        isPlaying = isPlaying,
                        isSaved = nowPlaying.isSaved,
                        isDownloading = nowPlaying.isDownloading,
                        downloadProgress = nowPlaying.downloadProgress,
                        onPlayOrPause = onPlayOrPause,
                        onBackward = onBackward,
                        onForward = onForward,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onSpeed = { showSpeedSheet = true },
                        speed = speed,
                        onSleepTimer = { showSleepTimerSheet = true },
                        sleepTimerRemainingMs = sleepTimerRemainingMs,
                        onList = { showPlaylistSheet = true },
                        onToggleSave = onToggleSave,
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        item {
            EpisodeInfoSection(
                episode = nowPlaying
            )
        }

        if (chapters.isNotEmpty()) {
            item {
                ChapterSection(
                    chapters = chapters,
                    selectedChapterIndex = chapterIndex,
                    onChapterClick = { chapter ->
                        onSeekTo(chapter.startTime.toLongMillis())
                    },
                )
            }
        }

        item {
            PodcastInfoSection(
                podcast = podcast,
                onPodcastClick = { onPodcastClick(podcast) },
                onToggleFollowed = { onToggleFollowedPodcast(podcast) }
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
            onToggleLikedEpisode = onToggleLikedEpisode,
            onToggleSavedEpisode = onToggleSavedEpisode,
            onDismiss = { showPlaylistSheet = false }
        )
    }

    if (showSleepTimerSheet) {
        SleepTimerSheet(
            remainingMs = sleepTimerRemainingMs,
            isPlaying = isPlaying,
            onSetTimer = { onSetSleepTimer(it) },
            onEndOfEpisode = onSleepTimerEndOfEpisode,
            onCancel = onCancelSleepTimer,
            onDismiss = { showSleepTimerSheet = false },
        )
    }
}

@Composable
fun PushUpCue(
    modifier: Modifier = Modifier,
    title: String,
) {
    val isVisible = title.isNotEmpty()

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface,
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY,
                    ),
                    shape = MaterialTheme.shapes.extraExtraLarge,
                ),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(
                targetState = title,
                transitionSpec = {
                    (slideInVertically(
                        initialOffsetY = { it }
                    ) + fadeIn()) togetherWith
                            (slideOutVertically(
                                targetOffsetY = { -it }
                            ) + fadeOut())
                },
                label = "push_up"
            ) { text ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = text,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ControlPanelProgress(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    progress: Progress,
    chapters: List<Chapter>,
    onSeekTo: (Long) -> Unit = {},
    onChapterIndex: (Int) -> Unit = {},
) {
    var chapterName by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        Column(
            modifier = Modifier,
        ) {
            EpisodiveSeeker(
                progress = progress,
                onSeekTo = onSeekTo,
                chapters = chapters,
                onChapterName = { chapterName = it },
                onChapterIndex = onChapterIndex,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.width(55.dp),
                    text = progress.position.toMediaTime(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )

                Text(
                    modifier = Modifier
                        .weight(1f),
                    text = chapterName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )

                Text(
                    modifier = Modifier.width(55.dp),
                    text = progress.duration.toMediaTime(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun ControlPanelBottom(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    isSaved: Boolean = false,
    isDownloading: Boolean = false,
    downloadProgress: Float = 0f,
    onPlayOrPause: () -> Unit = {},
    onBackward: () -> Unit = {},
    onForward: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onSpeed: () -> Unit = {},
    speed: Float,
    onSleepTimer: () -> Unit = {},
    sleepTimerRemainingMs: Long? = null,
    onList: () -> Unit = {},
    onToggleSave: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
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
                modifier = Modifier.size(48.dp),
                onClick = onBackward,
                icon = {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        imageVector = EpisodiveIcons.Replay15,
                        contentDescription = "Replay",
                    )
                }
            )

            EpisodiveIconButton(
                modifier = Modifier.size(48.dp),
                onClick = onPrevious,
                icon = {
                    Icon(
                        modifier = Modifier.size(40.dp),
                        imageVector = EpisodiveIcons.SkipPrevious,
                        contentDescription = "Previous",
                    )
                }
            )

            EpisodiveIconToggleButton(
                modifier = Modifier.size(68.dp),
                checked = isPlaying,
                onCheckedChange = { onPlayOrPause() },
                icon = {
                    Icon(
                        modifier = Modifier.size(36.dp),
                        imageVector = EpisodiveIcons.Play,
                        contentDescription = "Play",
                    )
                },
                checkedIcon = {
                    Icon(
                        modifier = Modifier.size(36.dp),
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
                modifier = Modifier.size(48.dp),
                onClick = onNext,
                icon = {
                    Icon(
                        modifier = Modifier.size(40.dp),
                        imageVector = EpisodiveIcons.SkipNext,
                        contentDescription = "Next",
                    )
                }
            )

            EpisodiveIconButton(
                modifier = Modifier.size(48.dp),
                onClick = onForward,
                icon = {
                    Icon(
                        modifier = Modifier.size(32.dp),
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
//                modifier = Modifier.size(56.dp),
                onClick = onSpeed,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    style = MaterialTheme.typography.titleLarge,
                    text = "${decimalFormat.format(speed)}x"
                )
            }

            EpisodiveIconButton(
                modifier = Modifier.size(32.dp),
                onClick = onSleepTimer,
                icon = {
                    val moonTint = when {
                        sleepTimerRemainingMs == null -> MaterialTheme.colorScheme.onSurface
                        sleepTimerRemainingMs <= PlayerViewModel.FADE_OUT_DURATION_MS -> {
                            val fraction = sleepTimerRemainingMs / PlayerViewModel.FADE_OUT_DURATION_MS.toFloat()
                            lerp(
                                MaterialTheme.colorScheme.onSurface,
                                MaterialTheme.colorScheme.primary,
                                fraction,
                            )
                        }
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = EpisodiveIcons.Moon,
                        contentDescription = stringResource(R.string.feature_player_sleep_timer),
                        tint = moonTint,
                    )
                }
            )

            if (isDownloading) {
                EpisodiveIconProgressButton(
                    modifier = Modifier.size(32.dp),
                    onClick = { onToggleSave() },
                    progress = downloadProgress,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    icon = {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = EpisodiveIcons.DownloadDone,
                            contentDescription = "Downloading",
                        )
                    },
                )
            } else {
                EpisodiveIconToggleButton(
                    modifier = Modifier.size(32.dp),
                    checked = isSaved,
                    onCheckedChange = { onToggleSave() },
                    colors = IconButtonDefaults.iconToggleButtonColors(
                        checkedContainerColor = Color.Transparent,
                        checkedContentColor = MaterialTheme.colorScheme.onSurface,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    icon = {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = EpisodiveIcons.Download,
                            contentDescription = "Save",
                        )
                    },
                    checkedIcon = {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = EpisodiveIcons.DownloadDone,
                            contentDescription = "Unsave",
                        )
                    }
                )
            }

            EpisodiveIconButton(
                modifier = Modifier.size(32.dp),
                onClick = onList,
                icon = {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = EpisodiveIcons.TransitionTop,
                        contentDescription = "List",
                    )
                }
            )
        }
    }
}

@Composable
private fun CardSection(
    modifier: Modifier = Modifier,
    title: String,
    expanded: Boolean,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .animateContentSize(),
            onClick = onClick,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 24.dp),
            ) {
                EpisodiveViewToggleButton(
                    modifier = Modifier.padding(bottom = 4.dp),
                    expanded = expanded,
                    onExpandedChange = { onClick() },
                    contentPadding = PaddingValues(0.dp),
                    text = {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                )

                content()
            }
        }
    }
}

@Composable
private fun EpisodeInfoSection(
    modifier: Modifier = Modifier,
    episode: Episode,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    CardSection(
        modifier = modifier,
        title = stringResource(R.string.feature_player_episode_info),
        expanded = isExpanded,
        onClick = { isExpanded = !isExpanded }
    ) {
        Text(
            text = episode.datePublished.toHumanReadable(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        HtmlTextContainer(
            text = episode.description ?: ""
        ) {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PodcastInfoSection(
    modifier: Modifier = Modifier,
    podcast: Podcast,
    onPodcastClick: () -> Unit = {},
    onToggleFollowed: () -> Unit = {},
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    CardSection(
        modifier = modifier,
        title = stringResource(R.string.feature_player_podcast_info),
        expanded = isExpanded,
        onClick = { isExpanded = !isExpanded }
    ) {
        HtmlTextContainer(
            text = podcast.description
        ) {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        PodcastSimpleItem(
            podcast = podcast,
            onClick = onPodcastClick,
            onToggleFollowed = onToggleFollowed,
        )
    }
}

@Composable
private fun ChapterSection(
    modifier: Modifier = Modifier,
    chapters: List<Chapter>,
    selectedChapterIndex: Int,
    onChapterClick: (Chapter) -> Unit = {},
) {
    val countLimit = 5
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    CardSection(
        modifier = modifier,
        title = stringResource(R.string.feature_player_chapter),
        expanded = isExpanded,
        onClick = { isExpanded = !isExpanded }
    ) {
        val displayedChapters = if (isExpanded) {
            chapters.withIndex().toList()
        } else {
            val startIndex = if (selectedChapterIndex < countLimit) {
                0
            } else {
                minOf(selectedChapterIndex - 2, chapters.size - countLimit).coerceAtLeast(0)
            }
            val endIndex = (startIndex + countLimit).coerceAtMost(chapters.size)
            chapters.withIndex().toList().subList(startIndex, endIndex)
        }

        displayedChapters.forEach { (index, chapter) ->
            ChapterItem(
                chapter = chapter,
                isSelected = index == selectedChapterIndex,
                onClick = { onChapterClick(chapter) }
            )
        }

        if (chapters.size > countLimit) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(
                    if (isExpanded) R.string.feature_player_show_less
                    else R.string.feature_player_show_more
                ),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PlaylistSheet(
    modifier: Modifier = Modifier,
    playlist: List<Episode>,
    playingIndex: Int,
    onEpisodeClick: (Episode) -> Unit = {},
    onToggleLikedEpisode: (Episode) -> Unit = {},
    onToggleSavedEpisode: (Episode) -> Unit = {},
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
                onToggleLikedEpisode = onToggleLikedEpisode,
                onToggleSavedEpisode = onToggleSavedEpisode,
            )
        }
    }
}

@Composable
private fun SleepTimerSheet(
    modifier: Modifier = Modifier,
    remainingMs: Long?,
    isPlaying: Boolean,
    onSetTimer: (Long) -> Unit = {},
    onEndOfEpisode: () -> Unit = {},
    onCancel: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val requiresPlaybackMessage = stringResource(R.string.feature_player_sleep_timer_requires_playback)

    fun handleAction(action: () -> Unit) {
        if (isPlaying) {
            action()
        } else {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = requiresPlaybackMessage,
                    duration = SnackbarDuration.Short,
                )
            }
        }
    }

    val timerPresets = remember {
        listOf(
            30L * 1000,
            5L * 60 * 1000,
            10L * 60 * 1000,
            15L * 60 * 1000,
            30L * 60 * 1000,
            45L * 60 * 1000,
            60L * 60 * 1000,
        )
    }
    // TODO: 30초 프리셋은 테스트용. 출시 시 제거
    val isActive = remainingMs != null

    ModalBottomSheet(
        modifier = modifier.fillMaxWidth(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { EpisodiveDragHandle() }
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.feature_player_sleep_timer),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val displayMs = remainingMs ?: 0L
                val minutes = displayMs / 1000 / 60
                val seconds = (displayMs / 1000) % 60
                val timerColor = when {
                    !isActive -> MaterialTheme.colorScheme.onSurface
                    displayMs <= PlayerViewModel.FADE_OUT_DURATION_MS -> {
                        val fraction = displayMs / PlayerViewModel.FADE_OUT_DURATION_MS.toFloat()
                        lerp(
                            MaterialTheme.colorScheme.onSurface,
                            MaterialTheme.colorScheme.primary,
                            fraction,
                        )
                    }
                    else -> MaterialTheme.colorScheme.primary
                }
                Text(
                    text = String.format("%d:%02d", minutes, seconds),
                    style = MaterialTheme.typography.headlineMedium,
                    color = timerColor,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    timerPresets.forEach { durationMs ->
                        val totalSeconds = (durationMs / 1000).toInt()
                        val label = if (totalSeconds < 60) "${totalSeconds}s"
                        else stringResource(R.string.feature_player_sleep_timer_minutes, totalSeconds / 60)
                        EpisodiveButton(
                            modifier = Modifier.size(48.dp),
                            onClick = { handleAction { onSetTimer(durationMs) } },
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            buttonColors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                }

                EpisodiveButton(
                    onClick = { handleAction { onEndOfEpisode() } },
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    buttonColors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                ) {
                    Text(
                        text = stringResource(R.string.feature_player_sleep_timer_end_of_episode),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                AnimatedVisibility(visible = isActive) {
                    EpisodiveButton(
                        onClick = { onCancel() },
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        buttonColors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.feature_player_sleep_timer_cancel),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            EpisodiveSwipeDismissSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
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
    val manualSpeed = remember { listOf(0.5f, 1f, 1.5f, 2f, 3.5f) }
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
                text = "${decimalFormat.format(speed)}${stringResource(R.string.feature_player_speed)}",
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
            progress = Progress(1000.seconds, 2000.seconds, 6000.seconds),
            isPlaying = true,
            onCollapse = {},
            onToggleLike = {},
            onToggleSave = {},
            onSeekTo = {},
            onPlayOrPause = {},
            onBackward = {},
            onForward = {},
            onPrevious = {},
            onNext = {},
            onPodcastClick = {},
            playlist = episodeTestDataList,
            indexOfList = 0,
            onEpisodeClick = {},
            onPlayIndex = {},
            onToggleLikedEpisode = {},
            speed = 1f,
            onSpeedChange = {},
            chapters = listOf(
                Chapter("Chapter 1", 0.seconds, 500.seconds),
                Chapter("Chapter 2", 500.seconds, 1500.seconds),
                Chapter("Chapter 3", 1500.seconds, 2500.seconds),
            ),
            onToggleFollowedPodcast = {},
            cue = "we start again after a rejection or a perceived",
        )
    }
}

@DevicePreviews
@Composable
private fun PushUpCuePreview() {
    EpisodiveTheme {
        PushUpCue(
            title = "we start again after a rejection or a perceived"
        )
    }
}

@DevicePreviews
@Composable
private fun EpisodeInfoSectionPreview() {
    EpisodiveTheme {
        EpisodeInfoSection(episode = episodeTestData)
    }
}

@DevicePreviews
@Composable
private fun PodcastInfoSectionPreview() {
    EpisodiveTheme {
        PodcastInfoSection(podcast = podcastTestData)
    }
}

@DevicePreviews
@Composable
private fun ChapterSectionPreview() {
    EpisodiveTheme {
        ChapterSection(
            chapters = listOf(
                Chapter("Chapter 1", 0.seconds, 500.seconds),
                Chapter("Chapter 2", 500.seconds, 1500.seconds),
                Chapter("Chapter 3", 1500.seconds, 2500.seconds),
            ),
            selectedChapterIndex = 0,
            onChapterClick = {}
        )
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
