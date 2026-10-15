package io.jacob.episodive.feature.radio

import androidx.annotation.StringRes
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.jacob.episodive.core.designsystem.component.EpisodiveScaffold
import io.jacob.episodive.core.designsystem.component.SectionHeader
import io.jacob.episodive.core.designsystem.component.SkeletonContainer
import io.jacob.episodive.core.designsystem.component.SkeletonLine
import io.jacob.episodive.core.designsystem.component.scrollbar.DraggableScrollbar
import io.jacob.episodive.core.designsystem.component.scrollbar.scrollbarState
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.screen.ErrorScreen
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.isRetryable
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.testing.model.regionalRadioStationTestData
import io.jacob.episodive.core.ui.StationCardStatus
import io.jacob.episodive.core.ui.StationRow
import io.jacob.episodive.core.ui.StationsSection
import io.jacob.episodive.core.ui.StationsSectionSkeleton
import io.jacob.episodive.core.ui.asUiMessage
import io.jacob.episodive.core.ui.stationCardStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import io.jacob.episodive.core.ui.R as uiR

@Composable
internal fun RadioRoute(
    modifier: Modifier = Modifier,
    viewModel: RadioViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val programImages by viewModel.programImages.collectAsStateWithLifecycle()

    val favoritedMessage = stringResource(R.string.feature_radio_snackbar_favorited)
    val unfavoritedMessage = stringResource(R.string.feature_radio_snackbar_unfavorited)
    val undoLabel = stringResource(uiR.string.core_ui_snackbar_undo)

    LaunchedEffect(Unit) {
        // 즐겨찾기 스낵바는 최신 것 하나만 남긴다. onShowSnackbar 는 스낵바가 닫힐 때까지 돌아오지
        // 않아, 그대로 기다리면 연타한 만큼 스낵바가 줄을 서서 하나씩 뜬다. 따로 띄워 두고 새 이펙트가
        // 오면 앞의 것을 취소한다. 이펙트 수집 자체는 collectLatest 로 바꾸지 않는다.
        var favoriteSnackbar: Job? = null
        viewModel.effect.collect { effect ->
            when (effect) {
                is RadioEffect.NavigateBack -> onBackClick()

                is RadioEffect.ShowFavoriteSnackbar -> {
                    favoriteSnackbar?.cancel()
                    favoriteSnackbar = launch {
                        val message = if (effect.isFavorite) favoritedMessage else unfavoritedMessage
                        val undone = onShowSnackbar(message, undoLabel)
                        if (undone) viewModel.sendAction(RadioAction.ToggleFavorite(effect.stationId))
                    }
                }
            }
        }
    }

    RadioScreen(
        modifier = modifier.fillMaxSize(),
        state = state,
        programImages = programImages,
        onBackClick = { viewModel.sendAction(RadioAction.ClickBack) },
        onStationClick = { viewModel.sendAction(RadioAction.ClickStation(it)) },
        onToggleFavorite = { viewModel.sendAction(RadioAction.ToggleFavorite(it)) },
        onRetry = { viewModel.sendAction(RadioAction.Retry) },
    )
}

@Composable
internal fun RadioScreen(
    modifier: Modifier = Modifier,
    state: RadioState,
    programImages: Map<String, String> = emptyMap(),
    onBackClick: () -> Unit,
    onStationClick: (Station) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRetry: () -> Unit,
) {
    EpisodiveScaffold(
        modifier = modifier,
        title = stringResource(R.string.feature_radio_title),
        titleStyle = MaterialTheme.typography.titleMedium,
        // 스테이션 행이 길게 이어지는 화면이라 홈 "더 보기" 와 같이 내려가는 동안 제목을 비킨다.
        // 뒤로가기는 계속 남는다.
        hideTitleOnScroll = true,
        navigationIcon = EpisodiveIcons.ArrowBack,
        navigationIconContentDescription = "Back",
        onNavigationClick = onBackClick,
    ) { paddingValues, nestedScrollConnection ->
        when (state) {
            is RadioState.Loading -> RadioSkeleton(
                paddingValues = paddingValues,
                nestedScrollConnection = nestedScrollConnection,
            )

            is RadioState.Success -> RadioList(
                paddingValues = paddingValues,
                nestedScrollConnection = nestedScrollConnection,
                state = state,
                programImages = programImages,
                onStationClick = onStationClick,
                onToggleFavorite = onToggleFavorite,
            )

            is RadioState.Error -> ErrorScreen(
                modifier = Modifier.padding(paddingValues),
                message = state.error.asUiMessage(),
                onRetry = if (state.error.isRetryable) onRetry else null,
            )
        }
    }
}

@Composable
private fun RadioList(
    paddingValues: PaddingValues,
    nestedScrollConnection: NestedScrollConnection,
    state: RadioState.Success,
    programImages: Map<String, String>,
    onStationClick: (Station) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    val dimension = LocalDimensionTheme.current
    val statusOf = { station: Station ->
        stationCardStatus(
            id = station.id,
            nowPlayingId = state.nowPlayingStationId,
            isPlaying = state.isPlaying,
            isBuffering = state.isBuffering,
            pendingId = state.pendingStationId,
        )
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // 아래 LazyColumn 이 내보내는 항목 수와 같아야 스크롤바 위치가 맞는다. 섹션을 더하거나 빼면
    // 여기도 함께 고친다.
    val itemCount = (if (state.favorites.isNotEmpty()) 1 else 0) +
        (if (state.recents.isNotEmpty()) 1 else 0) +
        state.nationwide.groupItemCount() +
        state.regional.groupItemCount() +
        1

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            state = listState,
            contentPadding = paddingValues.asContentPadding(),
            verticalArrangement = Arrangement.spacedBy(dimension.sectionSpacing),
        ) {
            // 같은 스테이션이 즐겨찾기·최근·전체에 함께 나온다. 키에 섹션 접두어를 붙여 겹치지 않게 한다.
            if (state.favorites.isNotEmpty()) {
                item(key = "fav", contentType = "carousel") {
                    StationsSection(
                        title = stringResource(R.string.feature_radio_section_favorites),
                        stations = state.favorites,
                        statusOf = statusOf,
                        onStationClick = onStationClick,
                        imageUrlOf = { programImages[it.id] },
                    )
                }
            }

            if (state.recents.isNotEmpty()) {
                item(key = "recent", contentType = "carousel") {
                    StationsSection(
                        title = stringResource(R.string.feature_radio_section_recent),
                        stations = state.recents,
                        statusOf = statusOf,
                        onStationClick = onStationClick,
                        imageUrlOf = { programImages[it.id] },
                    )
                }
            }

            stationGroup(
                headerKey = "header:nationwide",
                title = R.string.feature_radio_section_nationwide,
                stations = state.nationwide,
                state = state,
                statusOf = statusOf,
                programImages = programImages,
                onStationClick = onStationClick,
                onToggleFavorite = onToggleFavorite,
            )

            stationGroup(
                headerKey = "header:regional",
                title = R.string.feature_radio_section_regional,
                stations = state.regional,
                state = state,
                statusOf = statusOf,
                programImages = programImages,
                onStationClick = onStationClick,
                onToggleFavorite = onToggleFavorite,
            )

            item(key = "space") {
                // 미니플레이어 높이 + 마진만큼 비워야 마지막 항목이 가리지 않는다.
                Spacer(modifier = Modifier.height(dimension.playerBarSpace))
            }
        }

        listState.DraggableScrollbar(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 12.dp)
                .padding(top = paddingValues.calculateTopPadding())
                .align(Alignment.TopEnd),
            state = listState.scrollbarState(itemsAvailable = itemCount),
            orientation = Orientation.Vertical,
            onThumbMoved = { thumbPosition ->
                scope.launch {
                    val itemIndex = (thumbPosition * itemCount).toInt().coerceIn(0, itemCount - 1)
                    listState.scrollToItem(itemIndex)
                }
            },
        )
    }
}

/** [stationGroup] 이 내보내는 항목 수 — 비어 있으면 헤더도 없다. */
private fun List<Station>.groupItemCount(): Int = if (isEmpty()) 0 else size + 1

private fun LazyListScope.stationGroup(
    headerKey: String,
    @StringRes title: Int,
    stations: List<Station>,
    state: RadioState.Success,
    programImages: Map<String, String>,
    statusOf: (Station) -> StationCardStatus,
    onStationClick: (Station) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    if (stations.isEmpty()) return

    item(key = headerKey, contentType = "header") {
        SectionHeader(title = stringResource(title))
    }

    items(
        items = stations,
        key = { "all:${it.id}" },
        contentType = { "station" },
    ) { station ->
        StationRow(
            station = station,
            status = statusOf(station),
            isFavorite = station.id in state.favoriteIds,
            imageUrl = programImages[station.id],
            onClick = { onStationClick(station) },
            onToggleFavorite = { onToggleFavorite(station.id) },
        )
    }
}

@Composable
private fun RadioSkeleton(
    paddingValues: PaddingValues,
    nestedScrollConnection: NestedScrollConnection,
) {
    val dimension = LocalDimensionTheme.current

    SkeletonContainer(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .padding(top = paddingValues.calculateTopPadding()),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimension.sectionSpacing),
        ) {
            StationsSectionSkeleton()

            Column(
                modifier = Modifier.padding(horizontal = dimension.screenPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                repeat(5) {
                    SkeletonLine(style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/**
 * 스캐폴드가 준 여백을 목록의 contentPadding 으로 옮긴다. 탑바가 콘텐츠 위에 겹쳐 있어, 컨테이너
 * 패딩으로 두면 제목이 사라진 뒤에도 그 자리에 빈 띠가 남는다.
 */
@Composable
private fun PaddingValues.asContentPadding(): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current

    return PaddingValues(
        start = calculateStartPadding(layoutDirection),
        top = calculateTopPadding(),
        end = calculateEndPadding(layoutDirection),
        bottom = calculateBottomPadding(),
    )
}

@DevicePreviews
@Composable
private fun RadioScreenPreview() {
    EpisodiveTheme {
        RadioScreen(
            state = RadioState.Success(
                favorites = listOf(radioStationTestData),
                recents = listOf(radioStationTestData),
                nationwide = listOf(radioStationTestData),
                regional = listOf(regionalRadioStationTestData),
                favoriteIds = setOf(radioStationTestData.id),
                nowPlayingStationId = radioStationTestData.id,
                isPlaying = true,
                isBuffering = false,
                pendingStationId = null,
            ),
            onBackClick = {},
            onStationClick = {},
            onToggleFavorite = {},
            onRetry = {},
        )
    }
}
