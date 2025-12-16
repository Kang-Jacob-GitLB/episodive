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
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.model.mapper.toHumanReadable
import io.jacob.episodive.core.model.mapper.toLongMillis
import io.jacob.episodive.core.model.mapper.toMediaTime
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.ui.ChapterItem
import io.jacob.episodive.core.ui.PodcastSimpleItem
import io.jacob.episodive.core.ui.episodeItems
import kotlinx.coroutines.flow.collectLatest
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

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is PlayerEffect.NavigateToPodcast -> onPodcastClick(effect.podcast.id)
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
            onCollapse = { collapse() },
            onToggleLike = { viewModel.sendAction(PlayerAction.ToggleLike) },
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
            onToggleEpisodeLiked = { viewModel.sendAction(PlayerAction.ToggleEpisodeLiked(it)) },
            speed = s.speed,
            onSpeedChange = { viewModel.sendAction(PlayerAction.Speed(it)) },
            chapters = s.chapters,
            onTogglePodcastFollowed = { viewModel.sendAction(PlayerAction.TogglePodcastFollowed(it)) },
            cue = s.cue,
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
    onCollapse: () -> Unit,
    onToggleLike: () -> Unit,
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
    onToggleEpisodeLiked: (Episode) -> Unit,
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    chapters: List<Chapter>,
    onTogglePodcastFollowed: (Podcast) -> Unit,
    cue: String,
) {
    val listState = rememberLazyListState()
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
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
                        actionIcon = if (isLike) EpisodiveIcons.LikeFilled else EpisodiveIcons.Like,
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
                        onPlayOrPause = onPlayOrPause,
                        onBackward = onBackward,
                        onForward = onForward,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onSpeed = { showSpeedSheet = true },
                        speed = speed,
                        onList = { showPlaylistSheet = true },
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
                onToggleFollowed = { onTogglePodcastFollowed(podcast) }
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
            isLike = false,
            onCollapse = {},
            onToggleLike = {},
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
            onToggleEpisodeLiked = {},
            speed = 1f,
            onSpeedChange = {},
            chapters = listOf(
                Chapter("Chapter 1", 0.seconds, 500.seconds),
                Chapter("Chapter 2", 500.seconds, 1500.seconds),
                Chapter("Chapter 3", 1500.seconds, 2500.seconds),
            ),
            onTogglePodcastFollowed = {},
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