package io.jacob.episodive.feature.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.jacob.episodive.core.designsystem.component.EpisodiveDragHandle
import io.jacob.episodive.core.designsystem.component.EpisodiveIconButton
import io.jacob.episodive.core.designsystem.component.SectionHeader
import io.jacob.episodive.core.designsystem.component.SkeletonBox
import io.jacob.episodive.core.designsystem.component.SkeletonContainer
import io.jacob.episodive.core.designsystem.component.SkeletonCover
import io.jacob.episodive.core.designsystem.component.SkeletonLine
import io.jacob.episodive.core.designsystem.component.StateImage
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.screen.ErrorScreen
import io.jacob.episodive.core.designsystem.theme.EpisodiveHeroGradientEnd
import io.jacob.episodive.core.designsystem.theme.EpisodiveShapes
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.EpisodiveHeroGradientStart
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.coverUrl
import io.jacob.episodive.core.model.isRetryable
import io.jacob.episodive.core.model.mapper.toHumanReadable
import io.jacob.episodive.core.testing.model.channelTestDataList
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.liveEpisodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.ui.ChannelSection
import io.jacob.episodive.core.ui.ChannelSectionSkeleton
import io.jacob.episodive.core.ui.R as uiR
import io.jacob.episodive.core.ui.EpisodesSection
import io.jacob.episodive.core.ui.EpisodesSectionSkeleton
import io.jacob.episodive.core.ui.LocalIsPlaying
import io.jacob.episodive.core.ui.LocalNowPlayingEpisodeId
import io.jacob.episodive.core.ui.PodcastsSection
import io.jacob.episodive.core.ui.PodcastsSectionSkeleton
import io.jacob.episodive.core.ui.StationCardStatus
import io.jacob.episodive.core.ui.StationsSection
import io.jacob.episodive.core.ui.StationsSectionSkeleton
import io.jacob.episodive.core.ui.asUiMessage
import io.jacob.episodive.core.ui.stationCardStatus
import io.jacob.episodive.feature.home.navigation.HomeSection


/** 라디오 섹션의 목록 키. [HomeSection] 이 아니라 따로 둔다 — 그 enum 은 '더 보기' 목적지다. */
private const val HomeRadioSectionKey = "radio"

@Composable
internal fun HomeRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onPodcastClick: (Long) -> Unit,
    onChannelClick: (Long) -> Unit,
    onMoreClick: (HomeSection) -> Unit,
    onRadioClick: () -> Unit,
    onSearchClick: () -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
    radioViewModel: HomeRadioViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val radio by radioViewModel.state.collectAsStateWithLifecycle()
    val radioImages by radioViewModel.programImages.collectAsStateWithLifecycle()

    val unsavedMessage = stringResource(uiR.string.core_ui_snackbar_unsaved)
    val undoLabel = stringResource(uiR.string.core_ui_snackbar_undo)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToPodcast -> onPodcastClick(effect.podcastId)
                is HomeEffect.NavigateToChannel -> onChannelClick(effect.channelId)
                is HomeEffect.NavigateToMore -> onMoreClick(effect.section)
                is HomeEffect.ShowUnsaveSnackbar -> {
                    val undone = onShowSnackbar(unsavedMessage, undoLabel)
                    if (undone) viewModel.sendAction(HomeAction.ToggleSavedEpisode(effect.episode))
                }
            }
        }
    }

    when (val s = state) {
        is HomeState.Loading -> HomeSkeleton(onSearchClick = onSearchClick)

        is HomeState.Success -> HomeScreen(
            modifier = modifier
                .fillMaxSize(),
            playingEpisodes = s.playingEpisodes,
            userRecentPodcasts = s.userRecentPodcasts,
            randomEpisodes = s.randomEpisodes,
            userTrendingPodcasts = s.userTrendingPodcasts,
            followedPodcasts = s.followedPodcasts,
            localTrendingPodcasts = s.localTrendingPodcasts,
            foreignTrendingPodcasts = s.foreignTrendingPodcasts,
            liveEpisodes = s.liveEpisodes,
            channels = s.channels,
            // 오류는 빈 응답과 똑같이 섹션을 건너뛴다(SectionState.Error 의 관례).
            radioEntranceOnly = (radio as? HomeRadioState.Success)?.isEntranceOnly == true,
            radio = when (val r = radio) {
                is HomeRadioState.Loading -> SectionState.Loading
                is HomeRadioState.Success -> SectionState.Success(r.stations)
                is HomeRadioState.Error -> SectionState.Error(r.error)
            },
            radioStatusOf = { station ->
                val r = radio as? HomeRadioState.Success
                stationCardStatus(
                    id = station.id,
                    nowPlayingId = r?.nowPlayingStationId,
                    isPlaying = r?.isPlaying == true,
                    isBuffering = r?.isBuffering == true,
                    pendingId = r?.pendingStationId,
                )
            },
            radioImageOf = { radioImages[it.id] },
            onStationClick = { radioViewModel.sendAction(HomeRadioAction.ClickStation(it)) },
            onRadioMore = onRadioClick,
            onPlayEpisode = { viewModel.sendAction(HomeAction.PlayEpisode(it)) },
            onResumeEpisode = { viewModel.sendAction(HomeAction.ResumeEpisode(it)) },
            onToggleLikedEpisode = { viewModel.sendAction(HomeAction.ToggleLikedEpisode(it)) },
            onToggleSavedEpisode = { viewModel.sendAction(HomeAction.ToggleSavedEpisode(it)) },
            onPodcastClick = { viewModel.sendAction(HomeAction.ClickPodcast(it)) },
            onChannelClick = { viewModel.sendAction(HomeAction.ClickChannel(it)) },
            onMoreClick = { viewModel.sendAction(HomeAction.ClickMore(it)) },
            // 검색은 홈 데이터를 건드리지 않는 탭 전환이라 ViewModel 을 거치지 않는다.
            onSearchClick = onSearchClick,
        )

        is HomeState.Error -> ErrorScreen(
            message = s.error.asUiMessage(),
            onRetry = if (s.error.isRetryable) {
                { viewModel.sendAction(HomeAction.Retry) }
            } else {
                null
            },
        )
    }
}

@Composable
internal fun HomeScreen(
    modifier: Modifier = Modifier,
    playingEpisodes: SectionState<Episode>,
    userRecentPodcasts: SectionState<Podcast>,
    randomEpisodes: SectionState<Episode>,
    userTrendingPodcasts: SectionState<Podcast>,
    followedPodcasts: SectionState<Podcast>,
    localTrendingPodcasts: SectionState<Podcast>,
    foreignTrendingPodcasts: SectionState<Podcast>,
    liveEpisodes: SectionState<Episode>,
    channels: SectionState<Channel>,
    // 기본값은 "섹션 없음" 이다. 라디오를 모르는 호출부는 이 섹션이 아예 보이지 않는다.
    radio: SectionState<Station> = SectionState.Success(emptyList()),
    // 해외 목록이 비어도 헤더와 "더 보기" 입구는 남긴다(라디오 화면으로 가는 유일한 길).
    radioEntranceOnly: Boolean = false,
    radioStatusOf: (Station) -> StationCardStatus = { StationCardStatus.Idle },
    radioImageOf: (Station) -> String? = { null },
    onStationClick: (Station) -> Unit = {},
    onRadioMore: () -> Unit = {},
    onPlayEpisode: (Episode) -> Unit,
    onResumeEpisode: (Episode) -> Unit,
    onToggleLikedEpisode: (Episode) -> Unit,
    onToggleSavedEpisode: (Episode) -> Unit = {},
    onPodcastClick: (Long) -> Unit,
    onChannelClick: (Long) -> Unit,
    // 섹션별로 콜백을 여덟 개 두지 않고 하나로 받는다 — 어느 섹션인지는 인자가 말해준다.
    onMoreClick: (HomeSection) -> Unit,
    onSearchClick: () -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenHeight = this.maxHeight

        val density = LocalDensity.current
        var topBarHeight by remember { mutableStateOf(80.dp) }
        var contentHeight by remember { mutableStateOf(0.dp) }

        val sheetExpandHeight = screenHeight - topBarHeight - 32.dp
        // 이어듣기 캐러셀 높이만큼만 시트를 내려 둔다. 시트를 위로 끌면 그 위를 덮어
        // 목록에 화면 전체를 쓸 수 있다.
        val sheetPartiallyExpandHeight = sheetExpandHeight - contentHeight

        val sheetState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded,
                skipHiddenState = true
            )
        )

        BottomSheetScaffold(
            modifier = Modifier.fillMaxSize(),
            scaffoldState = sheetState,
            topBar = {
                Column(
                    modifier = Modifier
                        .onSizeChanged { size ->
                            topBarHeight = with(density) { size.height.toDp() }
                        },
                ) {
                    HomeHeader(
                        title = stringResource(R.string.feature_home_title),
                        onSearchClick = onSearchClick,
                    )
                }
            },
            content = {
                Column(
                    modifier = Modifier
                        .animateContentSize()
                        .onSizeChanged { size ->
                            contentHeight = with(density) {
                                size.height.toDp().coerceIn(0.dp, HomeHeroMaxPeekHeight)
                            }
                        },
                ) {
                    // 이어듣기는 로컬 DB 만 읽어 사실상 즉시 도착하므로 로딩 자리를 따로
                    // 잡지 않는다. 애초에 있을 수도 없을 수도 있는 자리라, 스켈레톤을 그리면
                    // 이어듣을 것이 없는 사용자에게 없는 카드를 약속하는 꼴이 된다.
                    val playing = playingEpisodes.itemsOrEmpty

                    if (playing.isNotEmpty()) {
                        HomeContinueListeningRow(
                            episodes = playing,
                            onEpisodeClick = onResumeEpisode,
                        )

                        Spacer(modifier = Modifier.height(HomeHeroSheetGap))
                    }
                }
            },
            sheetPeekHeight = sheetPartiallyExpandHeight,
            sheetDragHandle = { EpisodiveDragHandle() },
            sheetContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = sheetExpandHeight)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        // 콘텐츠가 짧아 fling 이 대부분 가장자리에서 끝나는데, 이때 잔여
                        // 속도·드래그가 stretch 로 흡수되며 스크롤이 멈추는 순간 콘텐츠가
                        // 반대 방향으로 움찔거리므로 overscroll 을 사용하지 않는다.
                        // 상단 가장자리는 시트 드래그(collapse)가 피드백을 대신한다.
                        overscrollEffect = null,
                    ) {
                        // 응답이 비어 온 섹션은 제목만 덩그러니 남으므로 통째로 건너뛴다.
                        // 구분선은 섹션 뒤가 아니라 앞에 붙인다 — 그래야 앞 섹션이 비어도
                        // 선이 겹치지 않고, 마지막 섹션 뒤에 선이 남지도 않는다.
                        var sectionRendered = false
                        fun <T> section(
                            key: String,
                            state: SectionState<T>,
                            skeleton: @Composable LazyItemScope.() -> Unit,
                            emptyContent: (@Composable LazyItemScope.() -> Unit)? = null,
                            content: @Composable LazyItemScope.(List<T>) -> Unit,
                        ) {
                            // 비어 있어도 그릴 내용이 있는 섹션만 건너뛰지 않는다.
                            var showEmptyContent = false
                            val items = when (state) {
                                // 아직 오지 않았다 — 자리를 잡아 둔다. 비워 두면 도착하는
                                // 순간 아래가 통째로 밀려, 누르려던 카드가 손가락 밑에서
                                // 움직인다. 랜덤 에피소드는 위에서 둘째라 특히 그렇다.
                                is SectionState.Loading -> null

                                is SectionState.Success -> state.items.ifEmpty {
                                    if (emptyContent == null) return
                                    showEmptyContent = true
                                    state.items
                                }

                                // 실패는 빈 응답과 똑같이 건너뛴다. RemoteUpdater 가 예외를
                                // 올리는 것은 캐시조차 없을 때뿐이라 대신 보여줄 것이 없고,
                                // 섹션마다 오류 카드를 늘어놓는 것은 피드가 할 일이 아니다.
                                is SectionState.Error -> return
                            }

                            // 키가 있어야 한다. 섹션은 이제 로딩 중에 나타났다가 빈 응답으로
                            // 사라질 수 있어 목록의 인덱스가 밀리는데, 키가 없으면 Compose 가
                            // 슬롯 상태를 인덱스로 짝지어 **다른 섹션의 것을 물려준다** —
                            // 캐러셀 안의 rememberLazyListState 가 엉뚱한 섹션에 적용돼 절반쯤
                            // 넘겨 둔 스크롤이 다른 줄에 나타난다.
                            if (sectionRendered) {
                                item(key = "divider:$key") {
                                    HorizontalDivider(modifier = Modifier.padding(16.dp))
                                }
                            }
                            sectionRendered = true
                            item(key = "section:$key") {
                                when {
                                    showEmptyContent -> emptyContent?.invoke(this)
                                    items == null -> skeleton()
                                    else -> content(items)
                                }
                            }
                        }

                        section(
                            key = HomeSection.MyRecentPodcasts.name,
                            state = userRecentPodcasts,
                            skeleton = { PodcastsSectionSkeleton(hasAction = true) },
                        ) { items ->
                            PodcastsSection(
                                title = stringResource(R.string.feature_home_section_my_recent_feeds),
                                podcasts = items,
                                subtitleProvider = { it.ownerName.ifEmpty { it.author } },
                                onMore = { onMoreClick(HomeSection.MyRecentPodcasts) },
                                onPodcastClick = { feed ->
                                    onPodcastClick(feed.id)
                                }
                            )
                        }

                        section(
                            key = HomeSection.RandomEpisodes.name,
                            state = randomEpisodes,
                            skeleton = {
                                EpisodesSectionSkeleton(
                                    count = HomeEpisodeSkeletonCount,
                                    hasAction = true,
                                )
                            },
                        ) { items ->
                            EpisodesSection(
                                title = stringResource(R.string.feature_home_section_random_episodes),
                                episodes = items,
                                onEpisodeClick = onPlayEpisode,
                                onToggleLikedEpisode = onToggleLikedEpisode,
                                onToggleSavedEpisode = onToggleSavedEpisode,
                                onMore = { onMoreClick(HomeSection.RandomEpisodes) },
                            )
                        }

                        section(
                            key = HomeSection.MyTrendingPodcasts.name,
                            state = userTrendingPodcasts,
                            skeleton = { PodcastsSectionSkeleton(hasAction = true) },
                        ) { items ->
                            PodcastsSection(
                                title = stringResource(R.string.feature_home_section_my_trending_feeds),
                                podcasts = items,
                                subtitleProvider = { it.ownerName.ifEmpty { it.author } },
                                onMore = { onMoreClick(HomeSection.MyTrendingPodcasts) },
                                onPodcastClick = { feed ->
                                    onPodcastClick(feed.id)
                                }
                            )
                        }

                        section(
                            key = HomeSection.FollowedPodcasts.name,
                            state = followedPodcasts,
                            skeleton = { PodcastsSectionSkeleton(hasAction = true) },
                        ) { items ->
                            PodcastsSection(
                                title = stringResource(R.string.feature_home_section_followed_podcasts),
                                podcasts = items,
                                onMore = { onMoreClick(HomeSection.FollowedPodcasts) },
                                onPodcastClick = { podcast ->
                                    onPodcastClick(podcast.id)
                                }
                            )
                        }

                        section(
                            key = HomeRadioSectionKey,
                            state = radio,
                            skeleton = { StationsSectionSkeleton(hasAction = true) },
                            emptyContent = if (radioEntranceOnly) {
                                {
                                    val title = stringResource(R.string.feature_home_section_radio)
                                    SectionHeader(
                                        title = title,
                                        actionIcon = EpisodiveIcons.CaretRight,
                                        actionIconContentDescription =
                                            stringResource(uiR.string.core_ui_section_more_format, title),
                                        onActionClick = onRadioMore,
                                    )
                                }
                            } else {
                                null
                            },
                        ) { items ->
                            StationsSection(
                                title = stringResource(R.string.feature_home_section_radio),
                                stations = items,
                                statusOf = radioStatusOf,
                                onStationClick = onStationClick,
                                onMore = onRadioMore,
                                imageUrlOf = radioImageOf,
                            )
                        }

                        section(
                            key = HomeSection.LocalTrendingPodcasts.name,
                            state = localTrendingPodcasts,
                            skeleton = { PodcastsSectionSkeleton(hasAction = true) },
                        ) { items ->
                            PodcastsSection(
                                title = stringResource(R.string.feature_home_section_trending_in_local),
                                podcasts = items,
                                subtitleProvider = { it.ownerName.ifEmpty { it.author } },
                                onMore = { onMoreClick(HomeSection.LocalTrendingPodcasts) },
                                onPodcastClick = { feed ->
                                    onPodcastClick(feed.id)
                                }
                            )
                        }

                        section(
                            key = HomeSection.ForeignTrendingPodcasts.name,
                            state = foreignTrendingPodcasts,
                            skeleton = { PodcastsSectionSkeleton(hasAction = true) },
                        ) { items ->
                            PodcastsSection(
                                title = stringResource(R.string.feature_home_section_trending_in_foreign),
                                podcasts = items,
                                subtitleProvider = { it.ownerName.ifEmpty { it.author } },
                                onMore = { onMoreClick(HomeSection.ForeignTrendingPodcasts) },
                                onPodcastClick = { feed ->
                                    onPodcastClick(feed.id)
                                }
                            )
                        }

                        section(
                            key = HomeSection.LiveEpisodes.name,
                            state = liveEpisodes,
                            skeleton = {
                                EpisodesSectionSkeleton(
                                    count = HomeEpisodeSkeletonCount,
                                    hasAction = true,
                                )
                            },
                        ) { items ->
                            EpisodesSection(
                                title = stringResource(R.string.feature_home_section_live_episodes),
                                episodes = items,
                                onEpisodeClick = onPlayEpisode,
                                onToggleLikedEpisode = onToggleLikedEpisode,
                                onToggleSavedEpisode = onToggleSavedEpisode,
                                onMore = { onMoreClick(HomeSection.LiveEpisodes) },
                            )
                        }

                        section(
                            key = HomeSection.Channels.name,
                            state = channels,
                            skeleton = { ChannelSectionSkeleton(hasAction = true) },
                        ) { items ->
                            ChannelSection(
                                title = stringResource(R.string.feature_home_section_channels),
                                channels = items,
                                onChannelClick = onChannelClick,
                                onMore = { onMoreClick(HomeSection.Channels) },
                            )
                        }

                        item {
                            // 미니플레이어 높이 + 마진만큼 비워야 마지막 항목이 가리지 않는다.
                            val dimension = LocalDimensionTheme.current
                            Spacer(
                                modifier = Modifier.height(
                                    dimension.playerBarSpace,
                                )
                            )
                        }
                    }
                }
            },
        )
    }
}

/** 이어듣기 캐러셀이 바텀시트 밖으로 내밀 수 있는 최대 높이. */
private val HomeHeroMaxPeekHeight = 240.dp

/**
 * 이어듣기 캐러셀과 바텀시트 사이 간격. 시트는 자기 드래그 핸들(위아래 여백 포함 34dp)을
 * 이미 이고 있으므로 여기서 더 벌리면 둘 사이가 크게 비어 보인다.
 */
private val HomeHeroSheetGap = 6.dp

/**
 * 이어듣기 카드가 차지하는 화면 폭 비율. 1 보다 작게 두어 다음 카드가 오른쪽에 살짝
 * 걸치게 하고, 그것으로 옆으로 넘길 수 있다는 신호를 준다.
 */
private const val HomeHeroWidthFraction = 0.78f

/** 커버 팔레트가 준비되기 전 쓰는 이어듣기 카드 그라디언트 양 끝. */
private val HomeHeroFallbackStart = EpisodiveHeroGradientStart
private val HomeHeroFallbackEnd = EpisodiveHeroGradientEnd

/**
 * 추출색으로 카드 그라디언트 양 끝을 만들 때 검정 쪽으로 섞는 비율.
 * 시작 쪽도 조금 눌러 둔다 — 밝은 커버에서 뽑힌 색 위에서는 흰 제목이 묻힌다.
 */
private const val HomeHeroGradientStartBlend = 0.2f
private const val HomeHeroGradientEndBlend = 0.6f

/** 이어듣기 카드 안쪽 여백과 커버 크기. */
private val HomeHeroPadding = 14.dp
private const val HomeHeroCoverSizeDp = 60
private val HomeHeroCoverSize = HomeHeroCoverSizeDp.dp

/**
 * 이어듣기 카드에서 남은 시간 라벨이 차지할 수 있는 최대 폭. 진행바가 `weight` 로 나머지를 먹으므로
 * 상한이 없으면 큰 글꼴 설정에서 라벨이 줄을 다 차지하고 진행바가 0 폭으로 사라진다. 좁은 화면
 * (320dp)에서도 진행바에 절반 가까이 남도록 잡은 값이다.
 */
private val HomeHeroRemainLabelMaxWidth = 120.dp

/**
 * 아이콘 버튼은 48dp 터치 영역 안에 24dp 아이콘을 가운데 두므로 좌우로 12dp 씩 빈 자리를
 * 이미 갖고 있다. 헤더의 우측 여백에서 그만큼 덜어야 아이콘이 눈에 보이는 위치가 제목의
 * 좌측 여백과 맞는다 — 덜지 않으면 아이콘만 홀로 안쪽으로 들어가 보인다.
 */
private val HomeHeaderActionInset = 12.dp

@Composable
private fun HomeHeader(
    modifier: Modifier = Modifier,
    title: String,
    onSearchClick: () -> Unit = {},
) {
    val dimension = LocalDimensionTheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            // 홈은 BottomSheetScaffold의 topBar 슬롯을 쓰지 않아 인셋이 자동으로 오지 않는다.
            // 이게 없으면 인사말이 상태바 시계와 겹친다.
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(
                start = dimension.headerPadding,
                // 여백이 아이콘 자체 여백보다 좁은 테마가 생기면 음수가 되어 크래시한다.
                end = (dimension.headerPadding - HomeHeaderActionInset).coerceAtLeast(0.dp),
                top = 16.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            // 제목이 길어져도 검색 버튼을 밀어내지 않도록 남는 폭만 차지하게 둔다.
            modifier = Modifier.weight(1f),
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        EpisodiveIconButton(onClick = onSearchClick) {
            Icon(
                imageVector = EpisodiveIcons.Search,
                contentDescription = stringResource(R.string.feature_home_search),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * 이어듣기 캐러셀. 카드 한 장이 화면을 다 차지하지 않게 해 다음 카드가 살짝 보이고,
 * 스냅 스크롤로 한 장씩 넘어간다.
 */
@Composable
private fun HomeContinueListeningRow(
    modifier: Modifier = Modifier,
    episodes: List<Episode>,
    onEpisodeClick: (Episode) -> Unit,
) {
    val dimension = LocalDimensionTheme.current
    val lazyListState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(
        lazyListState = lazyListState,
        snapPosition = SnapPosition.Start,
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        state = lazyListState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(horizontal = dimension.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(dimension.carouselSpacing),
        // 짧은 캐러셀은 가장자리 stretch 가 릴리즈 시 움찔거림으로 남는다.
        overscrollEffect = null,
    ) {
        items(
            items = episodes,
            key = { it.id },
        ) { episode ->
            HomeContinueListeningHero(
                modifier = Modifier.fillParentMaxWidth(HomeHeroWidthFraction),
                episode = episode,
                onClick = { onEpisodeClick(episode) },
            )
        }
    }
}

@Composable
private fun HomeContinueListeningHero(
    modifier: Modifier = Modifier,
    episode: Episode,
    onClick: () -> Unit,
) {
    val dimension = LocalDimensionTheme.current
    val density = LocalDensity.current
    val remain = episode.remain
    val leftLabel = stringResource(uiR.string.core_ui_left)

    // 목록 항목(EpisodeItem)과 같은 통로로 판단한다. 다만 id 만으로는 일시정지가 구분되지 않아
    // 재생 여부를 함께 본다 — 이어듣기 목록에는 방금 튼 것도 있어, 어느 것이 실제로 울리고
    // 있는지가 라벨 없이는 드러나지 않는다.
    val isNowPlaying = episode.id == LocalNowPlayingEpisodeId.current && LocalIsPlaying.current

    // 남은 시간 표기는 "5분 1초" → "5분" 처럼 항목이 통째로 빠지며 폭이 줄어든다. 그 자리를
    // 매번 다시 재면 나머지를 weight 로 먹는 진행바가 같이 늘었다 줄었다 하며 재생 내내
    // 출렁인다. 한 번 잡은 폭을 최대치로 붙들어 진행바 폭을 고정한다.
    //
    // 되감기처럼 표기가 되레 길어지는 경로에서는 폭이 한 번 넓어지고 그대로 유지된다 — 그 뒤로
    // 진행바가 조금 좁아진 채 있지만, 매초 출렁이는 것보다는 낫다는 판단이다. 폭 자체는
    // [HomeHeroRemainLabelMaxWidth] 로 막혀 있어 진행바가 사라지는 데까지 가지는 않는다.
    var remainLabelWidth by remember(episode.id) { mutableStateOf(0.dp) }

    // 카드 배경은 이 에피소드 커버에서 뽑은 색으로 흐른다. 팔레트가 준비되기 전에는
    // 기존 브랜드 그라디언트를 그대로 쓴다.
    var dominantColor by remember(episode.id) { mutableStateOf<Color?>(null) }
    val heroGradient = remember(dominantColor) {
        val start = dominantColor
            ?.let { lerp(it, Color.Black, HomeHeroGradientStartBlend) }
            ?: HomeHeroFallbackStart
        val end = dominantColor
            ?.let { lerp(it, Color.Black, HomeHeroGradientEndBlend) }
            ?: HomeHeroFallbackEnd
        Brush.linearGradient(listOf(start, end))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(heroGradient)
            .clickable(onClick = onClick)
            .padding(HomeHeroPadding),
    ) {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StateImage(
                    modifier = Modifier
                        .size(HomeHeroCoverSize)
                        .clip(EpisodiveShapes.coverForSize(HomeHeroCoverSizeDp)),
                    imageUrl = episode.coverUrl,
                    contentDescription = episode.title,
                    // 카드 그라디언트를 이 커버 색으로 물들이기 위해 위로 올린다.
                    onDominantColorExtracted = { dominantColor = it },
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(
                            if (isNowPlaying) uiR.string.core_ui_now_playing
                            else uiR.string.core_ui_continue
                        ),
                        // 원본은 11/700/.05em (원본 줄 182). labelMedium(13)은 두 단계 크다.
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.05.em),
                        // 두 라벨 모두 tertiary 다 — Color.kt 가 이 토큰을 "이어 듣기 · 재생 중 표시"에
                        // 배정하고 있어, 강조하겠다고 primary 로 바꾸면 디자인 시스템과 어긋난다.
                        color = MaterialTheme.colorScheme.tertiary,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 남은 시간은 진행바 아래가 아니라 재생 버튼이 있던 오른쪽 끝에 둔다.
            // 한 줄로 합치면서 카드 높이도 그만큼 줄어든다.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .weight(1f)
                        .height(dimension.progressThickness)
                        .clip(CircleShape),
                    progress = { episode.progress },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.16f),
                    // M3 기본 gapSize 4dp 를 끄지 않으면 채움과 트랙 사이가 끊겨 보인다.
                    // 원본은 끊김 없는 한 줄이다 (원본 줄 185).
                    gapSize = (-4).dp,
                    drawStopIndicator = {},
                )

                Text(
                    modifier = Modifier
                        .widthIn(
                            min = remainLabelWidth,
                            max = HomeHeroRemainLabelMaxWidth,
                        )
                        .onSizeChanged { size ->
                            val width = with(density) { size.width.toDp() }
                            if (width > remainLabelWidth) remainLabelWidth = width
                        },
                    // 목록 행과 같은 표기를 쓴다. 분 단위로 직접 만들면 로케일이 안 붙고
                    // 1분 미만이 "0분 남음"이 된다.
                    text = remain?.let { "${it.toHumanReadable()} $leftLabel" }.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    // 상한에 걸린 표기는 잘린다. 기본값 Clip 은 글자를 반 토막 내므로 잘렸다는
                    // 사실이 드러나는 말줄임을 쓴다.
                    overflow = TextOverflow.Ellipsis,
                    // 자리를 붙들어 두면 짧아진 표기가 그 안에서 왼쪽으로 쏠린다. 카드 오른쪽
                    // 가장자리에 맞춰 두려면 끝 정렬이어야 한다.
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

/** 이어듣기 히어로 자리가 처음 측정되기 전까지 시트 배경이 잡을 임시 높이. */
private val HomeHeroSkeletonInitialHeight = 130.dp

/**
 * 아직 오지 않은 에피소드 섹션이 잡아 둘 카드 수.
 *
 * `EpisodesSection` 은 가로 캐러셀이 아니라 세로 Column 이라 **이 수가 곧 섹션 높이**다.
 * 그래서 실제로 채워질 개수를 베껴 적지 않고 [HomeViewModel.COMPACT_MAX] 를 그대로 참조한다 —
 * 숫자를 두 벌로 두면 한쪽만 고쳤을 때 로딩 자리와 콘텐츠 높이가 어긋나 목록이 튄다.
 * 팟캐스트·채널 캐러셀은 넘치는 만큼이 가로로 화면 밖이라 이 값이 필요 없고, 각 스켈레톤의
 * 기본값을 그대로 쓴다.
 */
private const val HomeEpisodeSkeletonCount = HomeViewModel.COMPACT_MAX

/**
 * 로딩 자리. 화면 제목은 데이터와 무관한 정적 크롬이라 그대로 렌더하고, 이어듣기 히어로와
 * 시트 안 섹션 2개(캐러셀 1 + 세로 리스트 1)만 흉내낸다. 8개 섹션을 다 그리면 뒤쪽은 아무도
 * 못 보면서 컴포지션 비용만 내고, `section()` 헬퍼(L230-241)가 빈 섹션을 스킵하는 탓에 실제
 * 화면은 보통 3~5개 섹션만 뜨는데 8개를 그리면 스크롤 길이가 절반으로 줄며 스크롤바가 튄다.
 */
@Composable
private fun HomeSkeleton(
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
) {
    val dimension = LocalDimensionTheme.current
    val density = LocalDensity.current

    // 시트 배경이 시작할 위치. 실제 화면(L159-165)과 같은 패턴으로, 히어로 자리를 측정해
    // 첫 프레임 이후 정확한 값으로 갱신한다 — 텍스트 줄 수에 따라 높이가 바뀌므로 고정값을
    // 넣으면 어긋난다.
    var heroAreaHeight by remember { mutableStateOf(HomeHeroSkeletonInitialHeight) }

    Column(modifier = modifier.fillMaxSize()) {
        // 검색은 홈 데이터가 아직 없어도 갈 수 있는 곳이라, 로딩 중에도 눌리게 둔다.
        HomeHeader(
            title = stringResource(R.string.feature_home_title),
            onSearchClick = onSearchClick,
        )

        Box(modifier = Modifier.fillMaxSize()) {
            // 시트 배경은 SkeletonContainer 바깥의 별도 노드에서 먼저 칠한다. 안에서 칠하면
            // shimmerSweep 의 오프스크린 레이어 전체가 "그려진 픽셀"이 되어 화면이 통째로
            // 균일하게 쓸린다.
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = heroAreaHeight),
                shape = EpisodiveShapes.bottomSheet,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {}

            SkeletonContainer(modifier = Modifier.fillMaxSize()) {
                Column {
                    Column(
                        modifier = Modifier.onSizeChanged { size ->
                            heroAreaHeight = with(density) { size.height.toDp() }
                        },
                    ) {
                        HomeContinueListeningHeroSkeleton(
                            modifier = Modifier
                                .fillMaxWidth(HomeHeroWidthFraction)
                                .padding(start = dimension.screenPadding),
                        )

                        Spacer(modifier = Modifier.height(HomeHeroSheetGap))
                    }

                    // 드래그 핸들은 데이터와 무관한 정적 크롬이라 실제 컴포저블을 그대로 쓴다.
                    // 슬롯 밖이라 부모 정렬을 받지만, 컴포넌트가 스스로 가운데에 선다.
                    EpisodiveDragHandle()

                    Spacer(modifier = Modifier.height(16.dp))

                    // 캐러셀 1개 + 세로 리스트 1개. 어떤 섹션인지 특정하지 않는 중립적 모양으로
                    // 그린다 — 첫 섹션이 "나의 최근 피드"(신규 사용자는 빈 값)일 수도, "랜덤
                    // 에피소드"일 수도 있다.
                    // 실제 섹션에는 전부 더 보기 액션이 붙으므로 그 자리를 미리 잡는다.
                    // 없으면 데이터가 채워질 때 헤더가 커지며 아래가 통째로 밀린다.
                    PodcastsSectionSkeleton(count = 3, hasAction = true)

                    HorizontalDivider(modifier = Modifier.padding(16.dp))

                    EpisodesSectionSkeleton(count = 3, hasAction = true)
                }
            }
        }
    }
}

/**
 * 이어듣기 카드([HomeContinueListeningHero])와 같은 폭·안쪽 여백을 쓰되 그라디언트 배경은
 * 칠하지 않는다 — [SkeletonContainer] 안에서 배경을 칠하면 오프스크린 레이어 전체가 "그려진
 * 픽셀"이 되어 화면이 통째로 균일하게 쓸린다.
 */
@Composable
private fun HomeContinueListeningHeroSkeleton(modifier: Modifier = Modifier) {
    val dimension = LocalDimensionTheme.current

    Column(modifier = modifier.padding(HomeHeroPadding)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonCover(
                size = HomeHeroCoverSize,
                shape = EpisodiveShapes.coverForSize(HomeHeroCoverSizeDp),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                SkeletonLine(
                    style = MaterialTheme.typography.labelSmall,
                    widthFraction = 0.3f,
                )

                Spacer(modifier = Modifier.height(4.dp))

                SkeletonLine(
                    style = MaterialTheme.typography.titleSmall,
                    widthFraction = 0.9f,
                )
                SkeletonLine(
                    style = MaterialTheme.typography.titleSmall,
                    widthFraction = 0.55f,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SkeletonBox(
                modifier = Modifier
                    .weight(1f)
                    .height(dimension.progressThickness),
                shape = CircleShape,
            )

            SkeletonLine(
                modifier = Modifier.width(56.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@DevicePreviews
@Composable
private fun HomeSkeletonPreview() {
    EpisodiveTheme {
        HomeSkeleton()
    }
}

@DevicePreviews
@Composable
private fun HomeScreenPreview() {
    EpisodiveTheme {
        HomeScreen(
            playingEpisodes = SectionState.Success(episodeTestDataList),
            userRecentPodcasts = SectionState.Success(podcastTestDataList),
            randomEpisodes = SectionState.Success(episodeTestDataList),
            userTrendingPodcasts = SectionState.Success(podcastTestDataList),
            followedPodcasts = SectionState.Success(podcastTestDataList),
            localTrendingPodcasts = SectionState.Success(podcastTestDataList),
            foreignTrendingPodcasts = SectionState.Success(podcastTestDataList),
            liveEpisodes = SectionState.Success(liveEpisodeTestDataList),
            channels = SectionState.Success(channelTestDataList),
            onPlayEpisode = {},
            onResumeEpisode = {},
            onToggleLikedEpisode = {},
            onPodcastClick = {},
            onChannelClick = {},
            onMoreClick = {},
        )
    }
}
/**
 * 이 화면이 존재하는 이유가 되는 상태 — 일부 섹션만 아직 오지 않았다. 그 자리에는 스켈레톤이
 * 남고 이미 도착한 섹션은 정상적으로 눌린다.
 *
 * 캐러셀(팟캐스트·채널)과 세로 리스트(에피소드)를 하나씩 로딩으로 두어 두 종류의 로딩 자리를
 * 함께 본다. Robolectric 이 `lineHeight` 를 반영하지 않아 자리 예약은 테스트로 검증할 수 없고,
 * 이 미리보기와 기기가 그 역할을 한다.
 */
@DevicePreviews
@Composable
private fun HomeScreenPartiallyLoadedPreview() {
    EpisodiveTheme {
        HomeScreen(
            playingEpisodes = SectionState.Success(episodeTestDataList),
            userRecentPodcasts = SectionState.Success(podcastTestDataList),
            randomEpisodes = SectionState.Loading,
            userTrendingPodcasts = SectionState.Success(podcastTestDataList),
            followedPodcasts = SectionState.Success(podcastTestDataList),
            localTrendingPodcasts = SectionState.Loading,
            foreignTrendingPodcasts = SectionState.Success(podcastTestDataList),
            liveEpisodes = SectionState.Success(liveEpisodeTestDataList),
            channels = SectionState.Loading,
            onPlayEpisode = {},
            onResumeEpisode = {},
            onToggleLikedEpisode = {},
            onPodcastClick = {},
            onChannelClick = {},
            onMoreClick = {},
        )
    }
}
