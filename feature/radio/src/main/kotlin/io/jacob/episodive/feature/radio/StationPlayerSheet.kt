package io.jacob.episodive.feature.radio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.jacob.episodive.core.designsystem.component.EpisodiveIconButton
import io.jacob.episodive.core.designsystem.component.EpisodiveIconToggleButton
import io.jacob.episodive.core.designsystem.component.EpisodiveSwipeDismissSnackbarHost
import io.jacob.episodive.core.designsystem.component.FadingEdgeText
import io.jacob.episodive.core.designsystem.component.MorphIcon
import io.jacob.episodive.core.designsystem.component.PlayPauseMorphIcon
import io.jacob.episodive.core.designsystem.component.hiddenWhileLoading
import io.jacob.episodive.core.designsystem.component.playerBackdrop
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveShapes
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.SLEEP_TIMER_FADE_OUT_MS
import io.jacob.episodive.core.model.StationDetails
import io.jacob.episodive.core.model.art
import io.jacob.episodive.core.testing.model.kbsFirstRadioStationTestData
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.testing.model.regionalRadioStationTestData
import io.jacob.episodive.core.testing.model.stationDetailsTestData
import io.jacob.episodive.core.ui.LiveBadge
import io.jacob.episodive.core.ui.SleepTimerSheet
import io.jacob.episodive.core.ui.StationCover
import kotlinx.coroutines.launch
import io.jacob.episodive.core.ui.R as uiR

/** 하단 반투명 컨트롤 바 — 에피소드 플레이어와 같은 26dp 고정 반경. */
private val StationControlBarHeight = 52.dp
private val StationControlBarShape = RoundedCornerShape(26.dp)

/** 곡명 한 줄의 높이. 곡명이 있든 없든 이 높이를 비워 두어 아래 버튼이 움직이지 않게 한다. */
private val StreamTitleHeight = 24.dp

/** 지금 방송 중인 프로그램 줄의 자리 높이. 프로그램 정보 유무와 무관하게 비워 둔다. */
private val ProgramLineHeight = 22.dp

/**
 * 라디오 전용 플레이어 시트. 에피소드 플레이어(`PlayerBottomSheet`)와 같은 모양의 시트지만 탐색바·시간·배속이 없다
 * — 라이브라 갈 곳도 남은 시간도 없다.
 *
 * 닫는 경로는 셋이다. 사용자가 접거나, [collapseSignal] 이 오르거나, 재생이 끝나 상태가 [StationPlayerState.Hidden]
 * 이 되면 [onDismiss] 를 부른다.
 */
@Composable
fun StationPlayerSheet(
    collapseSignal: Int,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
    onOpenStationList: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StationPlayerViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val details by viewModel.details.collectAsStateWithLifecycle()
    val onAir by viewModel.onAir.collectAsStateWithLifecycle()
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    // 정지·감독자 포기·에피소드 전환·재시작 등 스테이션이 사라지는 모든 경로를 여기서 한 번에 받는다.
    LaunchedEffect(state is StationPlayerState.Hidden) {
        if (state is StationPlayerState.Hidden) currentOnDismiss()
    }

    // 마운트 시점 값을 기준으로 이후 증가분에만 반응한다. 시트를 막 연 직후 지난 신호로 닫히면 안 된다.
    val initialCollapse = remember { collapseSignal }
    LaunchedEffect(collapseSignal) {
        if (collapseSignal > initialCollapse) {
            sheetState.hide()
            currentOnDismiss()
        }
    }

    val s = state as? StationPlayerState.Active ?: return

    // 정지·접기는 시트가 내려간 뒤에 한다. 먼저 하면 시트가 사라지는 중에 내용이 비어 보인다.
    fun hideThen(action: () -> Unit) {
        scope.launch {
            sheetState.hide()
            action()
            currentOnDismiss()
        }
    }

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        scrimColor = Color.Transparent,
        // 시트 기본 배경을 그대로 두면 커버 그라디언트가 끝나는 지점부터 색이 한 단 밝게 끊긴다.
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = { WindowInsets(0) },
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = true,
        ),
    ) {
        Box {
            StationPlayerScreen(
                state = s,
                details = details,
                onAir = onAir,
                onCollapse = { hideThen {} },
                onPlayOrPause = { viewModel.sendAction(StationPlayerAction.PlayOrPause) },
                onPrevious = { viewModel.sendAction(StationPlayerAction.Previous) },
                onNext = { viewModel.sendAction(StationPlayerAction.Next) },
                onToggleFavorite = { viewModel.sendAction(StationPlayerAction.ToggleFavorite) },
                onSetSleepTimer = { viewModel.sendAction(StationPlayerAction.SetSleepTimer(it)) },
                onCancelSleepTimer = { viewModel.sendAction(StationPlayerAction.CancelSleepTimer) },
                onStop = { hideThen { viewModel.sendAction(StationPlayerAction.Stop) } },
                onOpenStationList = { hideThen(onOpenStationList) },
            )

            EpisodiveSwipeDismissSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
internal fun StationPlayerScreen(
    state: StationPlayerState.Active,
    modifier: Modifier = Modifier,
    details: StationDetailsOf? = null,
    onAir: OnAirOf? = null,
    onCollapse: () -> Unit = {},
    onPlayOrPause: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onSetSleepTimer: (Long) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    onStop: () -> Unit = {},
    onOpenStationList: () -> Unit = {},
) {
    val dimension = LocalDimensionTheme.current
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val station = state.station
    val art = remember(station) { station.art }
    // 늦게 오는 이전 스테이션의 프로그램은 그리지 않는다.
    val program = onAir?.takeIf { it.stationId == station.id }?.program
    var showSleepTimerSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // 짧은 콘텐츠가 가장자리에서 끝나며 움찔거리지 않게 한다. 에피소드 플레이어와 같다.
        overscrollEffect = null,
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillParentMaxHeight(0.92f)
                    .playerBackdrop(
                        top = Color(art.colorArgb),
                        background = MaterialTheme.colorScheme.background,
                    ),
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
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EpisodiveIconButton(
                            modifier = Modifier.size(dimension.iconButtonSize),
                            onClick = onCollapse,
                            icon = {
                                Icon(
                                    modifier = Modifier.size(22.dp),
                                    imageVector = EpisodiveIcons.CaretDown,
                                    contentDescription = stringResource(R.string.feature_radio_collapse),
                                )
                            }
                        )
                    }

                    // 남는 높이를 커버가 받는다. 작은 화면에서는 커버가 줄어들어 아래 컨트롤이 잘리지 않고, 큰 화면에서는
                    // 폭에 맞춘 정사각형이 이 칸 한가운데에 놓인다.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 26.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .shadow(
                                    elevation = 30.dp,
                                    shape = EpisodiveShapes.playerCover,
                                    ambientColor = Color.Black,
                                    spotColor = Color.Black,
                                ),
                        ) {
                            StationCover(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(EpisodiveShapes.playerCover),
                                station = station,
                                imageUrl = program?.imageUrl,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FadingEdgeText(
                            modifier = Modifier.fillMaxWidth(),
                            text = station.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )

                        FadingEdgeText(
                            modifier = Modifier.fillMaxWidth(),
                            text = station.region?.let { "${station.broadcaster} · $it" } ?: station.broadcaster,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )

                        // 프로그램 줄도 높이를 늘 비워 둔다. 프로그램 정보가 늦게 오거나 없어도 아래 버튼이 움직이지 않는다.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ProgramLineHeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            program?.let {
                                FadingEdgeText(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = it.titleWithHost(),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            LiveBadge()
                            Text(
                                text = stringResource(uiR.string.core_ui_live_now),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            program?.timeRangeText()?.let { range ->
                                Text(
                                    text = range,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                )
                            }
                        }

                        // 곡명이 없을 때도 높이를 비워 둔다. 있다 없다 하면 아래 버튼이 곡이 바뀔 때마다 움직인다.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(StreamTitleHeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            AnimatedContent(
                                // 곡을 주는 방송사(MBC)는 그것이 먼저다. 아니면 스트림이 싣는 제목.
                                targetState = (program?.song ?: state.streamTitle)?.takeIf { it.isNotBlank() },
                                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                                label = "StreamTitle",
                            ) { title ->
                                if (title != null) {
                                    FadingEdgeText(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }

                    StationTransportControls(
                        modifier = Modifier.padding(top = 12.dp),
                        state = state,
                        onPlayOrPause = onPlayOrPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                    )

                    StationControlBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimension.screenPadding)
                            .padding(top = 20.dp, bottom = 24.dp),
                        state = state,
                        onToggleFavorite = onToggleFavorite,
                        onSleepTimer = { showSleepTimerSheet = true },
                        onStop = onStop,
                        onOpenStationList = onOpenStationList,
                    )
                }
            }
        }

        // 항목 자리는 늘 둔다. 있다 없다 하면 위로 스크롤해 둔 채 이전/다음을 눌렀을 때 목록이 튄다.
        // 내용은 지금 보이는 스테이션의 상세일 때만 그린다.
        item {
            val shown = details?.takeIf { it.stationId == station.id }?.details
            if (shown != null) {
                StationInfoSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimension.screenPadding)
                        .padding(top = 8.dp, bottom = systemBarsPadding.calculateBottomPadding() + 32.dp),
                    details = shown,
                )
            }
        }
    }

    if (showSleepTimerSheet) {
        SleepTimerSheet(
            remainingMs = state.sleepTimerRemainingMs,
            isPlaying = state.isPlaying,
            // 라이브에는 끝이 없다.
            showEndOfEpisode = false,
            onSetTimer = onSetSleepTimer,
            onCancel = onCancelSleepTimer,
            onDismiss = { showSleepTimerSheet = false },
        )
    }
}

@Composable
private fun StationTransportControls(
    state: StationPlayerState.Active,
    onPlayOrPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimension = LocalDimensionTheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp),
        horizontalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EpisodiveIconButton(
            modifier = Modifier.size(56.dp),
            onClick = onPrevious,
            enabled = state.previous != null,
            icon = {
                Icon(
                    modifier = Modifier.size(28.dp),
                    imageVector = EpisodiveIcons.SkipPrevious,
                    contentDescription = stringResource(R.string.feature_radio_previous),
                )
            }
        )

        EpisodiveIconToggleButton(
            modifier = Modifier.size(dimension.playButtonSize),
            checked = state.isPlaying,
            onCheckedChange = { onPlayOrPause() },
            icon = {
                // 주소를 받는 동안(최대 15초)에는 플레이어가 아직 아무것도 버퍼링하지 않아 isPlaying 과 isBuffering 이
                // 모두 거짓이다. 받는 중도 준비 중으로 보여 누른 재생이 먹힌 것을 알린다.
                val isLoading = state.isPending || (!state.isPlaying && state.isBuffering)
                Box(contentAlignment = Alignment.Center) {
                    // 스피너가 아이콘 자리를 빼앗지 않고 위에 겹친다. 갈래를 나누면 준비를 거쳐 재생이 시작될 때
                    // 아이콘이 새로 붙어 재생→일시정지가 움직이지 못한다.
                    Box(modifier = if (isLoading) Modifier.hiddenWhileLoading() else Modifier) {
                        PlayPauseMorphIcon(
                            modifier = Modifier.size(34.dp),
                            isPlaying = state.isPlaying,
                        )
                    }
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(30.dp)
                                .semantics { contentDescription = "Loading" },
                            color = LocalContentColor.current,
                            strokeWidth = 3.dp,
                        )
                    }
                }
            },
            colors = IconButtonDefaults.iconToggleButtonColors(
                checkedContainerColor = MaterialTheme.colorScheme.onSurface,
                checkedContentColor = MaterialTheme.colorScheme.surface,
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            )
        )

        EpisodiveIconButton(
            modifier = Modifier.size(56.dp),
            onClick = onNext,
            enabled = state.next != null,
            icon = {
                Icon(
                    modifier = Modifier.size(28.dp),
                    imageVector = EpisodiveIcons.SkipNext,
                    contentDescription = stringResource(R.string.feature_radio_next),
                )
            }
        )
    }
}

@Composable
private fun StationControlBar(
    state: StationPlayerState.Active,
    onToggleFavorite: () -> Unit,
    onSleepTimer: () -> Unit,
    onStop: () -> Unit,
    onOpenStationList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFavorite = state.isFavorite
    val remainingMs = state.sleepTimerRemainingMs

    Box(
        modifier = modifier
            .height(StationControlBarHeight)
            .clip(StationControlBarShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), StationControlBarShape),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            // 슬롯을 똑같이 나눠 각 아이콘이 자기 칸 중앙에 오게 한다.
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EpisodiveIconToggleButton(
                modifier = Modifier.weight(1f),
                checked = isFavorite,
                onCheckedChange = { onToggleFavorite() },
                colors = IconButtonDefaults.iconToggleButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedContainerColor = Color.Transparent,
                    checkedContentColor = MaterialTheme.colorScheme.primary,
                ),
                icon = {
                    // 스테이션이 바뀌면 새 아이콘으로 시작한다. 이어 두면 다음 스테이션의 즐겨찾기 여부가 다를 때
                    // 누르지도 않은 모핑이 돈다.
                    key(state.station.id) {
                        MorphIcon(
                            modifier = Modifier.size(21.dp),
                            imageVector = if (isFavorite) EpisodiveIcons.FavoriteFilled else EpisodiveIcons.Favorite,
                            contentDescription = stringResource(
                                if (isFavorite) {
                                    uiR.string.core_ui_station_favorite_remove
                                } else {
                                    uiR.string.core_ui_station_favorite_add
                                }
                            ),
                            tint = if (isFavorite) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                },
            )

            EpisodiveIconButton(
                modifier = Modifier.weight(1f),
                onClick = onSleepTimer,
                icon = {
                    val moonTint = when {
                        remainingMs == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        remainingMs <= SLEEP_TIMER_FADE_OUT_MS -> lerp(
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            MaterialTheme.colorScheme.primary,
                            remainingMs / SLEEP_TIMER_FADE_OUT_MS.toFloat(),
                        )
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Icon(
                        modifier = Modifier.size(21.dp),
                        imageVector = EpisodiveIcons.Moon,
                        contentDescription = stringResource(uiR.string.core_ui_sleep_timer),
                        tint = moonTint,
                    )
                },
            )

            EpisodiveIconButton(
                modifier = Modifier.weight(1f),
                onClick = onStop,
                icon = {
                    Icon(
                        modifier = Modifier.size(21.dp),
                        imageVector = EpisodiveIcons.Stop,
                        contentDescription = stringResource(R.string.feature_radio_stop),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )

            EpisodiveIconButton(
                modifier = Modifier.weight(1f),
                onClick = onOpenStationList,
                icon = {
                    Icon(
                        modifier = Modifier.size(21.dp),
                        imageVector = EpisodiveIcons.List,
                        contentDescription = stringResource(R.string.feature_radio_station_list),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }
}

/**
 * 스테이션 부가 정보. 홈페이지·음질·태그 중 있는 것만 그린다. 커뮤니티가 채운 값이라 빠진 줄이 흔하다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StationInfoSection(
    details: StationDetails,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val quality = listOfNotNull(
        details.codec,
        details.bitrateKbps?.let { stringResource(R.string.feature_radio_info_bitrate_format, it) },
    ).joinToString(" · ").takeIf { it.isNotEmpty() }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HorizontalDivider()

        details.homepage?.let { homepage ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(
                        onClickLabel = stringResource(R.string.feature_radio_info_open_homepage),
                        role = Role.Button,
                    ) {
                        // 열 브라우저가 없는 기기에서 던진다. 부가 정보 때문에 앱이 죽으면 안 된다.
                        runCatching { uriHandler.openUri(homepage) }
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = EpisodiveIcons.WorldShare,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = homepage.toDisplayHost(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        quality?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (details.tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                details.tags.forEach { tag ->
                    Text(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        text = tag,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.feature_radio_info_source),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val DisplayHostPrefix = Regex("^(https?://)?(www\\.)?", RegexOption.IGNORE_CASE)

/** 주소 줄에는 스킴과 `www.` 를 떼고 보인다. 열 때는 원래 주소를 그대로 쓴다. */
private fun String.toDisplayHost(): String {
    return replace(DisplayHostPrefix, "").trimEnd('/')
}

@DevicePreviews
@Composable
private fun StationPlayerScreenPreview() {
    EpisodiveTheme {
        StationPlayerScreen(
            state = StationPlayerState.Active(
                station = radioStationTestData,
                isPending = false,
                streamTitle = "Artist - Title",
                isPlaying = true,
                isBuffering = false,
                isFavorite = true,
                previous = kbsFirstRadioStationTestData,
                next = regionalRadioStationTestData,
                sleepTimerRemainingMs = null,
            ),
            details = StationDetailsOf(radioStationTestData.id, stationDetailsTestData),
        )
    }
}
