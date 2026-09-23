package io.jacob.episodive.feature.player

import android.text.format.Formatter
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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.jacob.episodive.core.designsystem.component.EpisodiveDial
import io.jacob.episodive.core.designsystem.component.EpisodiveDragHandle
import io.jacob.episodive.core.designsystem.component.EpisodiveIconButton
import io.jacob.episodive.core.designsystem.component.EpisodiveIconProgressButton
import io.jacob.episodive.core.designsystem.component.EpisodiveSwipeDismissSnackbarHost
import io.jacob.episodive.core.designsystem.component.EpisodiveIconToggleButton
import io.jacob.episodive.core.designsystem.component.EpisodiveSeeker
import io.jacob.episodive.core.designsystem.component.EpisodiveTextButton
import io.jacob.episodive.core.designsystem.component.EpisodiveViewToggleHeader
import io.jacob.episodive.core.designsystem.component.FadingEdgeText
import io.jacob.episodive.core.designsystem.component.HtmlTextContainer
import io.jacob.episodive.core.designsystem.component.StateImage
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveShapes
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Chapter
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.model.caption.CaptionDownloadFailure
import io.jacob.episodive.core.model.caption.LiveCaption
import io.jacob.episodive.core.model.coverUrl
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
import io.jacob.episodive.core.ui.share.rememberShareLauncher

import kotlinx.coroutines.launch
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds

/** v2: 앨범아트 팔레트 색을 뽑기 전 기본 그라디언트 시작색(원본 줄 366). */
private val PlayerGradientDefaultTop = Color(0xFF7A3527)
private const val PlayerBackgroundGradientAngle = 193f

/** 그라디언트 중간·어두운 스톱이 커버 색에서 배경으로 섞이는 비율. */
private const val PlayerGradientMidBlend = 0.42f
private const val PlayerGradientDarkBlend = 0.78f

/** v2: 하단 반투명 컨트롤 바 — 5단계 shape 사다리에 없는 26px 고정 반경(원본 줄 380). */
private val PlayerControlBarHeight = 52.dp
private val PlayerControlBarShape = RoundedCornerShape(26.dp)
private val PlayerControlBarMarginTop = 8.dp
private val PlayerControlBarMarginBottom = 24.dp

/** 컨트롤 바 안에 들어가는 다운로드 진행률 링의 바깥 지름 — 바 높이보다 작아야 한다. */
private val PlayerDownloadRingSize = 40.dp

/** v2: 배속 다이얼 시트의 큰 숫자 — 타이포 스케일에 없는 일회성 크기(원본 줄 393). */
private val PlayerDialValueTextStyle = TextStyle(fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.02).em)
private val PlayerDialUnitTextStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Normal)

/** v2: 슬립 타이머 남은 시간 숫자 — 타이포 스케일에 없는 일회성 크기(원본 줄 413). */
private val PlayerSleepTimerValueTextStyle = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.02.em)

/**
 * 슬립 타이머 프리셋 원 지름. 목업 폭(344 - 좌우 22)에 다섯 개가 8dp 간격으로 한 줄에
 * 들어가는 크기다. 이보다 크면 좁은 기기에서 줄이 넘어간다.
 */
private val PlayerSleepTimerPresetSize = 52.dp

/** 재생목록 항목 여백 — 위아래는 첫·마지막 항목의 강조 배경이 잘리지 않을 만큼만 둔다. */
private val PlaylistHorizontalPadding = 16.dp
private val PlaylistVerticalPadding = 8.dp

@Composable
fun PlayerBottomSheet(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
    onPodcastClick: (Long) -> Unit,
    collapseSignal: Int = 0,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val state by viewModel.state.collectAsStateWithLifecycle()
    // caption 은 시트가 열려 있을 때만 수집한다(PlayerBar 는 절대 수집하지 않는다) — 이 구독이
    // 곧 인식 세션의 게이트다.
    val captionState = viewModel.caption.collectAsStateWithLifecycle()

    val unsavedMessage = stringResource(uiR.string.core_ui_snackbar_unsaved)
    val undoLabel = stringResource(uiR.string.core_ui_snackbar_undo)
    val sleepTimerExpiredMessage = stringResource(R.string.feature_player_sleep_timer_expired)
    val shareFailedMessage = stringResource(uiR.string.core_ui_share_failed)
    val deepLinkErrorMessage = stringResource(R.string.feature_player_deep_link_not_found)
    val captionDownloadStartedPattern = stringResource(R.string.feature_player_caption_download_started)
    val captionUnsupportedMessage = stringResource(R.string.feature_player_caption_unsupported)
    val captionFailedNetworkMessage = stringResource(R.string.feature_player_caption_download_failed_network)
    val captionFailedStorageMessage = stringResource(R.string.feature_player_caption_download_failed_storage)
    val captionFailedCorruptMessage = stringResource(R.string.feature_player_caption_download_failed_corrupt)
    val captionFailedUnloadableMessage = stringResource(R.string.feature_player_caption_download_failed_unloadable)
    val context = LocalContext.current

    val shareLauncher = rememberShareLauncher(
        onError = {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = shareFailedMessage,
                    duration = SnackbarDuration.Short,
                )
            }
        }
    )

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

                is PlayerEffect.ShowDeepLinkError -> {
                    snackbarHostState.showSnackbar(
                        message = deepLinkErrorMessage,
                        duration = SnackbarDuration.Short,
                    )
                }

                is PlayerEffect.CaptionDownloadStarted -> {
                    snackbarHostState.showSnackbar(
                        message = String.format(
                            captionDownloadStartedPattern,
                            Formatter.formatShortFileSize(context, effect.sizeBytes),
                        ),
                        duration = SnackbarDuration.Short,
                    )
                }

                is PlayerEffect.CaptionUnsupported -> {
                    snackbarHostState.showSnackbar(
                        message = captionUnsupportedMessage,
                        duration = SnackbarDuration.Short,
                    )
                }

                is PlayerEffect.CaptionDownloadFailed -> {
                    val message = when (effect.reason) {
                        CaptionDownloadFailure.Reason.NETWORK -> captionFailedNetworkMessage
                        CaptionDownloadFailure.Reason.STORAGE -> captionFailedStorageMessage
                        CaptionDownloadFailure.Reason.CORRUPT -> captionFailedCorruptMessage
                        CaptionDownloadFailure.Reason.UNLOADABLE -> captionFailedUnloadableMessage
                    }
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }

    // 위젯 feed 딥링크로 팟캐스트로 이동할 때, 열린 시트를 애니메이션으로 닫는다(앱 내 collapse 와 동일).
    // 마운트 시점 값을 기준으로 이후 증가분에만 반응해, 시트를 막 연 직후 stale 신호로 닫히는 것을 막는다.
    val initialCollapse = remember { collapseSignal }
    LaunchedEffect(collapseSignal) {
        if (collapseSignal > initialCollapse) {
            sheetState.hide()
            viewModel.sendAction(PlayerAction.CollapsePlayer)
        }
    }

    val s = state as? PlayerState.Success ?: return

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = { viewModel.sendAction(PlayerAction.CollapsePlayer) },
        sheetState = sheetState,
        dragHandle = null,
        scrimColor = Color.Transparent,
        // 시트 기본 배경(surfaceContainerLow)을 그대로 두면 커버 그라디언트가 끝나는
        // 지점부터 색이 한 단 밝게 끊긴다. 그라디언트의 마지막 스톱과 같은 색으로 맞춘다.
        containerColor = MaterialTheme.colorScheme.background,
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
            onShare = {
                shareLauncher.share(
                    episode = s.nowPlaying,
                    // 에피소드에 웹 링크가 없는 경우가 대부분이라(피드가 잘 주지 않는다)
                    // 팟캐스트 링크를 폴백으로 함께 넘긴다.
                    podcast = s.podcast,
                    // 지금 흐르는 위치가 이 에피소드의 것일 때만 싣는다. progress 는 에피소드가
                    // 바뀌는 순간 잠깐 이전 것을 들고 있어, 확인 없이 실으면 남의 지점을 보낸다.
                    // 다 들은 에피소드도 뺀다 — 위치가 끝에 멈춰 있어 "엔딩 크레딧부터 들어라"가
                    // 된다. 모델의 하한(MIN_SHARE_POSITION_MS)은 이 반대쪽만 막는다.
                    positionMs = s.progress.position.inWholeMilliseconds
                        .takeIf {
                            s.progress.episodeId == s.nowPlaying.id && !s.nowPlaying.isCompleted
                        },
                )
            },
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
            liveCaption = { captionState.value.caption },
            captionButtonState = { captionState.value.toCaptionButtonState() },
            onToggleCaption = { viewModel.sendAction(PlayerAction.ToggleCaption) },
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
    onShare: () -> Unit,
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
    liveCaption: () -> LiveCaption? = { null },
    captionButtonState: () -> CaptionButtonState = { CaptionButtonState.Inactive },
    onToggleCaption: () -> Unit = {},
) {
    val dimension = LocalDimensionTheme.current
    val listState = rememberLazyListState()
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var chapterIndex by remember { mutableStateOf(0) }
    var dominantColor by remember { mutableStateOf(PlayerGradientDefaultTop) }
    val backgroundColor = MaterialTheme.colorScheme.background


    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        state = listState,
        // 콘텐츠가 짧아 fling 이 대부분 가장자리에서 끝나는데, 이때 잔여 속도·드래그가
        // stretch 로 흡수되며 스크롤이 멈추는 순간 콘텐츠가 반대 방향으로 움찔거리므로
        // overscroll 을 사용하지 않는다. 상단 가장자리는 시트 드래그(dismiss)가 피드백을 대신한다.
        overscrollEffect = null,
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillParentMaxHeight(0.92f)
                    // v2: 193도 · 4단계 스톱 구조를 유지하되, 중간 스톱까지 전부 앨범아트
                    // 팔레트 색에서 파생시킨다. 고정 레드 스톱을 끼우면 커버가 무슨 색이든
                    // 배경이 늘 붉게 나온다.
                    .drawWithCache {
                        val (start, end) = angledGradientEndpoints(PlayerBackgroundGradientAngle, size)
                        val brush = Brush.linearGradient(
                            0f to dominantColor,
                            0.30f to lerp(dominantColor, backgroundColor, PlayerGradientMidBlend),
                            0.58f to lerp(dominantColor, backgroundColor, PlayerGradientDarkBlend),
                            0.82f to backgroundColor,
                            start = start,
                            end = end,
                        )
                        onDrawBehind { drawRect(brush) }
                    },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = systemBarsPadding.calculateTopPadding()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, start = 20.dp, end = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 커버 그라디언트 위에 원형 배경을 깔면 두 버튼만 뿌옇게 떠 보인다.
                        // 아이콘만 남기고 배경은 두지 않는다.
                        EpisodiveIconButton(
                            modifier = Modifier.size(dimension.iconButtonSize),
                            onClick = onCollapse,
                            icon = {
                                Icon(
                                    modifier = Modifier.size(22.dp),
                                    imageVector = EpisodiveIcons.CaretDown,
                                    contentDescription = "Down",
                                )
                            }
                        )

                        EpisodiveIconButton(
                            modifier = Modifier.size(dimension.iconButtonSize),
                            onClick = onToggleLike,
                            icon = {
                                Icon(
                                    modifier = Modifier.size(22.dp),
                                    imageVector = if (nowPlaying.isLiked) EpisodiveIcons.LikeFilled else EpisodiveIcons.Like,
                                    contentDescription = "Like",
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 26.dp)
                            .aspectRatio(1f)
                            .shadow(
                                elevation = 30.dp,
                                shape = EpisodiveShapes.playerCover,
                                ambientColor = Color.Black,
                                spotColor = Color.Black,
                            ),
                    ) {
                        StateImage(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(EpisodiveShapes.playerCover),
                            size = 600,
                            imageUrl = nowPlaying.coverUrl,
                            contentDescription = nowPlaying.title,
                            onDominantColorExtracted = { dominantColor = it },
                        )

                        CueOverlay(
                            modifier = Modifier
                                .align(Alignment.BottomCenter),
                            cue = cue,
                            liveCaption = liveCaption,
                            episodeId = progress.episodeId,
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FadingEdgeText(
                            modifier = Modifier
                                .fillMaxWidth(),
                            text = nowPlaying.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )

                        FadingEdgeText(
                            modifier = Modifier
                                .clickable { onPodcastClick(podcast) },
                            text = podcast.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }

                    ControlPanelProgress(
                        modifier = Modifier.padding(top = 16.dp),
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
                        onShare = onShare,
                        captionButtonState = captionButtonState,
                        onToggleCaption = onToggleCaption,
                    )
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

/** CSS `linear-gradient(angleDeg, ...)` 각도 규약을 Compose 선형 그라디언트의 시작/끝 좌표로 변환한다. */
private fun angledGradientEndpoints(angleDegrees: Float, size: Size): Pair<Offset, Offset> {
    val angleRad = Math.toRadians(angleDegrees.toDouble())
    val width = size.width
    val height = size.height
    val length = abs(width * sin(angleRad)) + abs(height * cos(angleRad))
    val centerX = width / 2f
    val centerY = height / 2f
    val dx = (length / 2 * sin(angleRad)).toFloat()
    val dy = (length / 2 * cos(angleRad)).toFloat()
    return Offset(centerX - dx, centerY + dy) to Offset(centerX + dx, centerY - dy)
}

/**
 * [PushUpCue] 한 번의 전환 대상 — 원문·번역이 함께 밀려 올라온다.
 *
 * `lineKey` 를 상태 안에 싣는 이유: `contentKey` 는 전환에 걸린 **각** 상태에 대해 불리므로
 * 그 상태 자신의 키를 돌려줘야 한다. 바깥 변수를 캡처하면 나가는 줄과 들어오는 줄이 같은
 * (현재) 키를 받아 줄이 바뀌어도 밀어올림이 일어나지 않는다.
 */
private data class PushUpCueContent(val title: String, val subtitle: String?, val lineKey: Any)

@Composable
fun PushUpCue(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    lineKey: Any = title,
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
                    // v2: 커버 하단 자막 오버레이 — 위(투명)에서 아래(검정 60%)로(원본 줄 371).
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f),
                        ),
                    ),
                    shape = EpisodiveShapes.playerCover,
                ),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(
                targetState = PushUpCueContent(title, subtitle, lineKey),
                // 같은 줄의 partial 갱신(lineKey 불변)은 contentKey 가 같아 애니메이션 없이
                // 제자리에서 텍스트만 바뀐다. lineKey 가 바뀔 때만 밀어올림이 일어난다.
                contentKey = { it.lineKey },
                transitionSpec = {
                    (slideInVertically(
                        initialOffsetY = { it }
                    ) + fadeIn()) togetherWith
                            (slideOutVertically(
                                targetOffsetY = { -it }
                            ) + fadeOut())
                },
                label = "push_up"
            ) { content ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = content.title,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (content.subtitle != null) {
                        Text(
                            modifier = Modifier.padding(top = 2.dp),
                            text = content.subtitle,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 커버 하단 자막 오버레이. [liveCaption] 은 람다로만 읽어 재구성을 이 컴포저블 안에 가둔다 —
 * 값으로 풀어 상위에서 넘기면 매 단어(partial)마다 상위 트리까지 재구성된다.
 *
 * [liveCaption] 이 있고 그 `episodeId` 가 [episodeId](= progress.episodeId)와 일치할 때만
 * 자막(원문+번역)을 그리고, 아니면 기존 transcript [cue] 를 그대로 보여준다. 두 경우 모두
 * [PushUpCue] 호출은 하나뿐이다 — if/else 로 두 번 부르면 AnimatedVisibility 상태가 리셋된다.
 */
@Composable
private fun CueOverlay(
    modifier: Modifier = Modifier,
    cue: String,
    liveCaption: () -> LiveCaption?,
    episodeId: Long?,
) {
    val caption = liveCaption()
    val matched = caption != null && caption.episodeId == episodeId

    PushUpCue(
        modifier = modifier,
        title = if (matched) caption!!.text else cue,
        subtitle = if (matched) caption!!.translation else null,
        lineKey = if (matched) caption!!.lineId else cue,
    )
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
            .padding(horizontal = 30.dp),
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
    onShare: () -> Unit,
    captionButtonState: () -> CaptionButtonState = { CaptionButtonState.Inactive },
    onToggleCaption: () -> Unit = {},
) {
    val dimension = LocalDimensionTheme.current

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 26.dp, end = 26.dp, top = 14.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EpisodiveIconButton(
                modifier = Modifier.size(48.dp),
                onClick = onBackward,
                icon = {
                    Icon(
                        modifier = Modifier.size(31.dp),
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
                        modifier = Modifier.size(25.dp),
                        imageVector = EpisodiveIcons.SkipPrevious,
                        contentDescription = "Previous",
                    )
                }
            )

            EpisodiveIconToggleButton(
                modifier = Modifier.size(dimension.playButtonSize),
                checked = isPlaying,
                onCheckedChange = { onPlayOrPause() },
                icon = {
                    Icon(
                        modifier = Modifier.size(34.dp),
                        imageVector = EpisodiveIcons.Play,
                        contentDescription = "Play",
                    )
                },
                checkedIcon = {
                    Icon(
                        modifier = Modifier.size(34.dp),
                        imageVector = EpisodiveIcons.Pause,
                        contentDescription = "Pause",
                    )
                },
                colors = IconButtonDefaults.iconToggleButtonColors(
                    checkedContainerColor = MaterialTheme.colorScheme.onSurface,
                    checkedContentColor = MaterialTheme.colorScheme.surface,
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                )
            )

            EpisodiveIconButton(
                modifier = Modifier.size(48.dp),
                onClick = onNext,
                icon = {
                    Icon(
                        modifier = Modifier.size(25.dp),
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
                        modifier = Modifier.size(31.dp),
                        imageVector = EpisodiveIcons.Forward30,
                        contentDescription = "Forward",
                    )
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimension.screenPadding)
                .padding(top = PlayerControlBarMarginTop, bottom = PlayerControlBarMarginBottom)
                .height(PlayerControlBarHeight)
                .clip(PlayerControlBarShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), PlayerControlBarShape),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                // SpaceAround 는 요소 사이 여백을 균등하게 두지만 요소 폭이 제각각이면
                // (여기서는 "1x" 텍스트만 넓다) 아이콘 중심 간격이 어긋난다. 슬롯을 똑같이
                // 나눠 각 아이콘이 자기 칸 중앙에 오게 한다.
                verticalAlignment = Alignment.CenterVertically
            ) {
                val decimalFormat = DecimalFormat("#.#")

                EpisodiveTextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onSpeed,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        text = "${decimalFormat.format(speed)}x"
                    )
                }

                EpisodiveIconButton(
                    modifier = Modifier.weight(1f),
                    onClick = onSleepTimer,
                    icon = {
                        val moonTint = when {
                            sleepTimerRemainingMs == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            sleepTimerRemainingMs <= PlayerViewModel.FADE_OUT_DURATION_MS -> {
                                val fraction = sleepTimerRemainingMs / PlayerViewModel.FADE_OUT_DURATION_MS.toFloat()
                                lerp(
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                    MaterialTheme.colorScheme.primary,
                                    fraction,
                                )
                            }
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Icon(
                            modifier = Modifier.size(21.dp),
                            imageVector = EpisodiveIcons.Moon,
                            contentDescription = stringResource(R.string.feature_player_sleep_timer),
                            tint = moonTint,
                        )
                    }
                )

                CaptionToggleButton(
                    modifier = Modifier.weight(1f),
                    buttonState = captionButtonState,
                    onClick = onToggleCaption,
                )

                // 여섯 칸 가운데. 왼쪽 셋(배속·타이머·자막)은 듣는 방식을, 오른쪽 셋(공유·
                // 다운로드·목록)은 에피소드를 다룬다 — 공유는 그 오른쪽 그룹의 첫 칸이다.
                EpisodiveIconButton(
                    modifier = Modifier.weight(1f),
                    onClick = onShare,
                    icon = {
                        Icon(
                            modifier = Modifier.size(21.dp),
                            imageVector = EpisodiveIcons.Share,
                            contentDescription = stringResource(uiR.string.core_ui_share),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                )

                if (isDownloading) {
                    EpisodiveIconProgressButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onToggleSave() },
                        // 컨트롤 바 높이(52dp) 안에 링이 들어가야 위아래가 잘리지 않는다.
                        size = PlayerDownloadRingSize,
                        // 크기 확정 전(progress 0)에는 무한 스피너, 진행률이 잡히면 실제 %로 표시
                        isLoading = downloadProgress <= 0f,
                        progress = downloadProgress,
                        // 활성 색은 같은 바에 있는 슬립 타이머와 같은 primary 다. 링만 색으로
                        // 표시하고 안쪽 원은 비워, 바 안의 다른 아이콘들과 같은 평면으로 둔다.
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                        icon = {
                            Icon(
                                modifier = Modifier.size(21.dp),
                                imageVector = EpisodiveIcons.Download,
                                contentDescription = "Downloading",
                            )
                        },
                    )
                } else {
                    EpisodiveIconToggleButton(
                        modifier = Modifier.weight(1f),
                        checked = isSaved,
                        onCheckedChange = { onToggleSave() },
                        colors = IconButtonDefaults.iconToggleButtonColors(
                            checkedContainerColor = Color.Transparent,
                            // 다운로드 완료(활성)도 슬립 타이머 활성과 같은 primary 로 맞춘다.
                            checkedContentColor = MaterialTheme.colorScheme.primary,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        icon = {
                            Icon(
                                modifier = Modifier.size(21.dp),
                                imageVector = EpisodiveIcons.Download,
                                contentDescription = "Save",
                            )
                        },
                        checkedIcon = {
                            Icon(
                                modifier = Modifier.size(21.dp),
                                imageVector = EpisodiveIcons.DownloadDone,
                                contentDescription = "Unsave",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    )
                }

                EpisodiveIconButton(
                    modifier = Modifier.weight(1f),
                    onClick = onList,
                    icon = {
                        Icon(
                            modifier = Modifier.size(21.dp),
                            imageVector = EpisodiveIcons.TransitionTop,
                            contentDescription = "List",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                )
            }
        }
    }
}

/**
 * 컨트롤 바 자막 토글. [buttonState] 는 람다로만 읽는다 — [CaptionButtonState] 는 자막 텍스트가
 * 아니라 켜짐 여부·가용성에만 좌우되지만, 같은 원본 `LiveCaptionState` 를 구독하는 이상 매
 * partial 갱신마다 `.value` 읽기 자체가 재구성을 유발한다. 그 재구성을 이 버튼 하나로 가둔다.
 */
@Composable
private fun CaptionToggleButton(
    modifier: Modifier = Modifier,
    buttonState: () -> CaptionButtonState,
    onClick: () -> Unit,
) {
    val turnOnDescription = stringResource(R.string.feature_player_caption_turn_on)
    val turnOffDescription = stringResource(R.string.feature_player_caption_turn_off)
    val downloadingDescription = stringResource(R.string.feature_player_caption_downloading)

    when (val state = buttonState()) {
        is CaptionButtonState.Progress -> EpisodiveIconProgressButton(
            modifier = modifier,
            onClick = onClick,
            size = PlayerDownloadRingSize,
            isLoading = state.progress <= 0f,
            progress = state.progress,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
            icon = {
                Icon(
                    modifier = Modifier.size(21.dp),
                    imageVector = EpisodiveIcons.Caption,
                    contentDescription = downloadingDescription,
                )
            },
        )

        CaptionButtonState.Active -> EpisodiveIconButton(
            modifier = modifier,
            onClick = onClick,
            icon = {
                Icon(
                    modifier = Modifier.size(21.dp),
                    imageVector = EpisodiveIcons.Caption,
                    contentDescription = turnOffDescription,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        )

        CaptionButtonState.Dimmed -> EpisodiveIconButton(
            modifier = modifier,
            onClick = onClick,
            icon = {
                Icon(
                    modifier = Modifier.size(21.dp),
                    imageVector = EpisodiveIcons.Caption,
                    contentDescription = turnOffDescription,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                )
            }
        )

        CaptionButtonState.Inactive -> EpisodiveIconButton(
            modifier = modifier,
            onClick = onClick,
            icon = {
                Icon(
                    modifier = Modifier.size(21.dp),
                    imageVector = EpisodiveIcons.Caption,
                    contentDescription = turnOnDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        )
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
                // 버튼이 아니라 표시용 헤더다. 카드 전체가 이미 클릭 대상이라 여기에
                // 버튼을 겹쳐 두면 클릭 지점이 둘로 갈린다 — 제목이나 아이콘을 누르면
                // 리플이 그 언저리에만 번지고, 스크린리더도 같은 동작을 두 번 읽는다.
                EpisodiveViewToggleHeader(
                    modifier = Modifier.padding(bottom = 4.dp),
                    expanded = expanded,
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
            .fillMaxHeight(0.78f)
            .windowInsetsPadding(WindowInsets.statusBars),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = EpisodiveShapes.bottomSheet,
        dragHandle = { EpisodiveDragHandle() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.feature_player_playlist_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = stringResource(R.string.feature_player_playlist_queue_count, playlist.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                // 재생 중 항목은 강조 배경이 행 바깥으로 번진다. 좌우 여백을 리스트에 걸면
                // 그 번짐이 리스트 경계에서 잘리므로 여백은 항목에 준다.
                contentPadding = PaddingValues(vertical = PlaylistVerticalPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                episodeItems(
                    itemModifier = Modifier.padding(horizontal = PlaylistHorizontalPadding),
                    episodes = playlist,
                    playingIndex = playingIndex,
                    onEpisodeClick = onEpisodeClick,
                    onToggleLikedEpisode = onToggleLikedEpisode,
                    onToggleSavedEpisode = onToggleSavedEpisode,
                )
            }
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

    // 한 줄에 들어가는 다섯 개까지만 둔다. 줄바꿈되면 시트가 세로로 늘어나 다이얼·버튼이 밀린다.
    // 커스텀 시간 입력이 없어 이 다섯 개가 선택지의 전부이므로, 칸을 잠들지 않을 시간에 쓰지 않는다.
    val timerPresets = remember {
        listOf(
            5L * 60 * 1000,
            10L * 60 * 1000,
            15L * 60 * 1000,
            30L * 60 * 1000,
            60L * 60 * 1000,
        )
    }
    val isActive = remainingMs != null

    ModalBottomSheet(
        modifier = modifier.fillMaxWidth(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = EpisodiveShapes.bottomSheet,
        dragHandle = { EpisodiveDragHandle() }
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(horizontal = 22.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.feature_player_sleep_timer),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
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
                val moonColor = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        modifier = Modifier.size(26.dp),
                        imageVector = EpisodiveIcons.Moon,
                        contentDescription = null,
                        tint = moonColor,
                    )

                    Text(
                        text = String.format("%d:%02d", minutes, seconds),
                        style = PlayerSleepTimerValueTextStyle,
                        color = timerColor,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    timerPresets.forEach { durationMs ->
                        // 단위는 로케일 리소스가 붙인다 (한국어 "분", 영어 "m").
                        val label = stringResource(
                            R.string.feature_player_sleep_timer_minutes,
                            (durationMs / 1000 / 60).toInt(),
                        )
                        PlayerPresetCircle(
                            size = PlayerSleepTimerPresetSize,
                            label = label,
                            selected = false,
                            onClick = { handleAction { onSetTimer(durationMs) } },
                        )
                    }
                }

                PlayerSheetActionButton(
                    onClick = { handleAction { onEndOfEpisode() } },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    icon = EpisodiveIcons.SkipNext,
                    text = stringResource(R.string.feature_player_sleep_timer_end_of_episode),
                )

                AnimatedVisibility(visible = isActive) {
                    PlayerSheetActionButton(
                        onClick = { onCancel() },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        icon = EpisodiveIcons.Close,
                        text = stringResource(R.string.feature_player_sleep_timer_cancel),
                        fontWeight = FontWeight.Bold,
                    )
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

    ModalBottomSheet(
        modifier = modifier
            .fillMaxWidth(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = EpisodiveShapes.bottomSheet,
        dragHandle = { EpisodiveDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.feature_player_playback_speed),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = decimalFormat.format(speed),
                    style = PlayerDialValueTextStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = stringResource(R.string.feature_player_speed),
                    style = PlayerDialUnitTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            EpisodiveDial(
                modifier = Modifier.padding(top = 6.dp),
                value = speed,
                onValueChange = onSpeedChange,
            )

            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                manualSpeed.forEach { presetSpeed ->
                    PlayerPresetCircle(
                        size = 52.dp,
                        label = "${decimalFormat.format(presetSpeed)}x",
                        selected = speed == presetSpeed,
                        onClick = { onSpeedChange(presetSpeed) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

    }
}

/** v2: 배속·슬립타이머 시트가 공유하는 원형 프리셋 (원본 줄 396, 415). */
@Composable
private fun PlayerPresetCircle(
    modifier: Modifier = Modifier,
    size: Dp,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(CircleShape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    // 시트 배경(surfaceContainer)과 한 단계 차이인 surfaceContainerHigh 로는
                    // 버튼 경계가 보이지 않는다. 두 단계 위 표면에 테두리까지 둘러 분리한다.
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
            )
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        CircleShape,
                    )
                },
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/** v2: 슬립타이머 시트의 "에피소드 끝까지" / "타이머 취소" pill 버튼(원본 줄 422~423). */
@Composable
private fun PlayerSheetActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector,
    text: String,
    fontWeight: FontWeight = FontWeight.SemiBold,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(EpisodiveShapes.pill)
            .background(containerColor)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = fontWeight),
            color = contentColor,
        )
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
            onShare = {},
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
