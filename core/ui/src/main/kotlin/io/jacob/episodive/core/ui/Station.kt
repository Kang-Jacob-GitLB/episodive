package io.jacob.episodive.core.ui

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.jacob.episodive.core.designsystem.component.EpisodiveIconToggleButton
import io.jacob.episodive.core.designsystem.component.MorphIcon
import io.jacob.episodive.core.designsystem.component.SectionHeader
import io.jacob.episodive.core.designsystem.component.SectionHeaderSkeleton
import io.jacob.episodive.core.designsystem.component.SkeletonBox
import io.jacob.episodive.core.designsystem.component.SkeletonLine
import io.jacob.episodive.core.designsystem.component.WaveAnimationIcon
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.tooling.ThemePreviews
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.art
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.testing.model.regionalRadioStationTestData

/** 스테이션 카드가 보여 줄 재생 상태. */
enum class StationCardStatus { Idle, Preparing, Playing, Paused }

/**
 * 카드 하나의 상태. [pendingId] 가 먼저다 — 주소를 받는 동안(최대 15초)에는 플레이어가 아직 아무것도
 * 버퍼링하지 않아 [isBuffering] 이 거짓이므로, 누른 직후의 피드백은 이 토큰만 줄 수 있다.
 */
fun stationCardStatus(
    id: String,
    nowPlayingId: String?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    pendingId: String?,
): StationCardStatus = when {
    pendingId == id || (nowPlayingId == id && isBuffering) -> StationCardStatus.Preparing
    nowPlayingId == id && isPlaying -> StationCardStatus.Playing
    nowPlayingId == id -> StationCardStatus.Paused
    else -> StationCardStatus.Idle
}

/** 모서리 반지름이 변 길이에 비례하는 이니셜 아트의 비율. 호출부가 같은 비율로 clip 한다. */
const val StationArtCornerRatio = 0.22f

private val StationTileSize = 96.dp
private val StationRowArtSize = 48.dp

/**
 * 스테이션 이니셜 아트. 정사각형이고 로고 대신 방송사 색 위에 이니셜을 얹는다.
 * 장식이라 접근성 트리에서는 뺀다 — 이름은 곁의 텍스트가 읽는다.
 */
@Composable
fun StationArt(
    station: Station,
    modifier: Modifier = Modifier,
) {
    val art = remember(station) { station.art }
    BoxWithConstraints(
        modifier = modifier
            .background(Color(art.colorArgb))
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        // 부모가 무한 제약을 주면 side 가 Dp.Infinity 가 되어 폰트 크기가 발산한다.
        val side = minOf(maxWidth, maxHeight).coerceAtMost(StationArtMaxSide)
        Text(
            text = art.initials,
            color = Color.White,
            fontSize = with(LocalDensity.current) { (side * art.textRatio).toSp() },
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** [StationCover] 의 프로그램 이미지 노드 태그. 이미지를 요청했는지 테스트가 가른다. */
const val StationCoverImageTag = "StationCoverImage"

/**
 * 스테이션 커버. 프로그램 이미지가 있으면 그것을 꽉 채워 그리고, 없거나 받는 동안·실패했을 때는 이니셜 아트
 * ([StationArt]) 를 그대로 둔다. 아트를 늘 아래에 깔아 두므로 이미지가 늦게 와도 빈 칸이 생기지 않는다.
 */
@Composable
fun StationCover(
    station: Station,
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        StationArt(station = station, modifier = Modifier.fillMaxSize())

        if (imageUrl != null && !LocalInspectionMode.current) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                // 장식이다. 프로그램 제목은 곁의 텍스트가 읽는다.
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(StationCoverImageTag),
            )
        }
    }
}

// 플레이어 시트의 큰 커버까지 비율대로 키울 수 있게 넉넉히 둔다. 무한 제약에서 폰트가 발산하는 것만 막으면 된다.
private val StationArtMaxSide = 480.dp

/** 아트 위에 상태를 얹은 정사각형. 상태가 [StationCardStatus.Idle] 이면 아트만 보인다. */
@Composable
private fun StationArtWithStatus(
    station: Station,
    status: StationCardStatus,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * StationArtCornerRatio)),
        contentAlignment = Alignment.Center,
    ) {
        StationArt(station = station, modifier = Modifier.fillMaxSize())

        if (status != StationCardStatus.Idle) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                StationStatusIndicator(status = status, size = size / 3)
            }
        }
    }
}

@Composable
private fun StationStatusIndicator(
    status: StationCardStatus,
    size: Dp,
) {
    when (status) {
        StationCardStatus.Idle -> Unit

        // PlayerBar 와 같은 설명을 단다. 테스트와 스크린리더가 이 문자열로 준비 중을 찾는다.
        StationCardStatus.Preparing -> CircularProgressIndicator(
            modifier = Modifier
                .size(size)
                .semantics { contentDescription = "Loading" },
            color = Color.White,
            strokeWidth = 2.dp,
        )

        // 막대는 장식이다. 상태는 카드의 stateDescription 이 말한다.
        StationCardStatus.Playing -> WaveAnimationIcon(
            modifier = Modifier
                .size(size)
                .clearAndSetSemantics {},
            color = Color.White,
            isAnimating = true,
        )

        StationCardStatus.Paused -> WaveAnimationIcon(
            modifier = Modifier
                .size(size)
                .clearAndSetSemantics {},
            color = Color.White,
            isAnimating = false,
        )
    }
}

/** 카드를 누르면 재생한다는 것과 지금 상태를 한 노드로 읽히게 한다. */
@Composable
private fun Modifier.stationCardSemantics(
    status: StationCardStatus,
    onClick: () -> Unit,
): Modifier {
    val playLabel = stringResource(R.string.core_ui_station_play)
    val stateText = when (status) {
        StationCardStatus.Idle -> null
        StationCardStatus.Preparing -> stringResource(R.string.core_ui_station_connecting)
        StationCardStatus.Playing -> stringResource(R.string.core_ui_now_playing)
        StationCardStatus.Paused -> stringResource(R.string.core_ui_station_paused)
    }
    return this
        .clickable(onClickLabel = playLabel, onClick = onClick)
        .semantics(mergeDescendants = true) {
            stateText?.let { stateDescription = it }
        }
}

@Composable
fun StationsSection(
    modifier: Modifier = Modifier,
    title: String,
    stations: List<Station>,
    statusOf: (Station) -> StationCardStatus,
    onStationClick: (Station) -> Unit,
    onMore: (() -> Unit)? = null,
) {
    SectionHeader(
        modifier = modifier,
        title = title,
        // 더 보기 어포던스는 onMore 유무로만 결정한다 — PodcastsSection 과 같은 계약이다.
        actionIcon = EpisodiveIcons.CaretRight.takeIf { onMore != null },
        actionIconContentDescription = onMore?.let {
            stringResource(R.string.core_ui_section_more_format, title)
        },
        onActionClick = onMore ?: {},
    ) {
        val dimension = LocalDimensionTheme.current
        val lazyListState = rememberLazyListState()
        val flingBehavior = rememberSnapFlingBehavior(
            lazyListState = lazyListState,
            snapPosition = SnapPosition.Start,
        )

        // 방금 튼 스테이션은 최근 기록에서 맨 앞으로 옮겨진다. LazyRow 는 키로 스크롤 위치를 지키므로
        // 그냥 두면 누른 카드가 왼쪽 밖으로 사라진다. 첫 항목이 바뀌면 앞으로 데려온다.
        // 최초 컴포지션에서는 움직이지 않는다 — 복원된 스크롤 위치를 덮으면 안 된다.
        val firstStationId = stations.firstOrNull()?.id
        var isFirstComposition by remember { mutableStateOf(true) }
        LaunchedEffect(firstStationId) {
            if (isFirstComposition) {
                isFirstComposition = false
            } else if (firstStationId != null) {
                lazyListState.animateScrollToItem(0)
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            state = lazyListState,
            flingBehavior = flingBehavior,
            horizontalArrangement = Arrangement.spacedBy(dimension.carouselSpacing),
            contentPadding = PaddingValues(horizontal = dimension.screenPadding),
            overscrollEffect = null,
        ) {
            items(
                items = stations,
                key = { it.id },
            ) { station ->
                StationTile(
                    modifier = Modifier.animateItem(),
                    station = station,
                    status = statusOf(station),
                    onClick = { onStationClick(station) },
                )
            }
        }
    }
}

/** [StationsSection] 로딩 자리. 헤더와 타일 개수만큼의 캐러셀을 그대로 흉내낸다. */
@Composable
fun StationsSectionSkeleton(
    modifier: Modifier = Modifier,
    count: Int = 3,
    hasAction: Boolean = false,
) {
    val dimension = LocalDimensionTheme.current

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        SectionHeaderSkeleton(hasAction = hasAction)

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            // 아직 데이터가 없는 캐러셀이라 끌리면 없는 콘텐츠를 만지는 것처럼 보인다.
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(dimension.carouselSpacing),
            contentPadding = PaddingValues(horizontal = dimension.screenPadding),
        ) {
            items(count) {
                StationTileSkeleton()
            }
        }
    }
}

/** 캐러셀 칸. 정사각형 아트 아래에 이름을 둔다. */
@Composable
fun StationTile(
    modifier: Modifier = Modifier,
    station: Station,
    status: StationCardStatus,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(StationTileSize)
            .then(modifier)
            .stationCardSemantics(status, onClick),
    ) {
        StationArtWithStatus(station = station, status = status, size = StationTileSize)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = station.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun StationTileSkeleton(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .width(StationTileSize)
            .then(modifier),
    ) {
        SkeletonBox(
            modifier = Modifier.size(StationTileSize),
            shape = RoundedCornerShape(StationTileSize * StationArtCornerRatio),
        )

        Spacer(modifier = Modifier.height(8.dp))

        SkeletonLine(style = MaterialTheme.typography.labelMedium)
        SkeletonLine(style = MaterialTheme.typography.labelMedium, widthFraction = 0.6f)
    }
}

/**
 * 세로 목록 행. 즐겨찾기 토글은 눌러서 재생하는 영역 밖의 형제로 둔다 — 같은 병합 노드 안에 두면
 * 스크린리더가 토글을 따로 찾지 못한다.
 */
@Composable
fun StationRow(
    modifier: Modifier = Modifier,
    station: Station,
    status: StationCardStatus,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val dimension = LocalDimensionTheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimension.screenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .stationCardSemantics(status, onClick)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StationArtWithStatus(station = station, status = status, size = StationRowArtSize)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = station.region ?: station.broadcaster,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        EpisodiveIconToggleButton(
            size = 40.dp,
            checked = isFavorite,
            onCheckedChange = { onToggleFavorite() },
            colors = IconButtonDefaults.iconToggleButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkedContainerColor = Color.Transparent,
                checkedContentColor = MaterialTheme.colorScheme.primary,
            ),
            icon = {
                // 한 자리에서 아이콘만 바꿔 넘겨야 외곽선 ↔ 채움이 이어서 움직인다.
                MorphIcon(
                    modifier = Modifier.size(22.dp),
                    imageVector = if (isFavorite) EpisodiveIcons.FavoriteFilled else EpisodiveIcons.Favorite,
                    contentDescription = stringResource(
                        if (isFavorite) {
                            R.string.core_ui_station_favorite_remove
                        } else {
                            R.string.core_ui_station_favorite_add
                        }
                    ),
                    tint = if (isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            },
        )
    }
}

@ThemePreviews
@Composable
private fun StationRowPreview() {
    EpisodiveTheme {
        Column {
            StationRow(
                station = radioStationTestData,
                status = StationCardStatus.Playing,
                isFavorite = true,
                onClick = {},
                onToggleFavorite = {},
            )
            StationRow(
                station = regionalRadioStationTestData,
                status = StationCardStatus.Idle,
                isFavorite = false,
                onClick = {},
                onToggleFavorite = {},
            )
        }
    }
}
