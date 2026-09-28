package io.jacob.episodive.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.paging.compose.LazyPagingItems
import io.jacob.episodive.core.designsystem.component.ClipAnimationIconText
import io.jacob.episodive.core.designsystem.component.EpisodiveIconButton
import io.jacob.episodive.core.designsystem.component.EpisodiveIconToggleButton
import io.jacob.episodive.core.designsystem.component.HtmlTextContainer
import io.jacob.episodive.core.designsystem.component.MorphIcon
import io.jacob.episodive.core.designsystem.component.PlayPauseMorphIcon
import io.jacob.episodive.core.designsystem.component.SectionHeader
import io.jacob.episodive.core.designsystem.component.SectionHeaderSkeleton
import io.jacob.episodive.core.designsystem.component.SkeletonBox
import io.jacob.episodive.core.designsystem.component.SkeletonCover
import io.jacob.episodive.core.designsystem.component.SkeletonLine
import io.jacob.episodive.core.designsystem.component.StateImage
import io.jacob.episodive.core.designsystem.component.SubSectionHeader
import io.jacob.episodive.core.designsystem.component.waveIdleBandLevel
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveShapes
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.designsystem.tooling.ThemePreviews
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.coverUrl
import io.jacob.episodive.core.model.mapper.toHumanReadable
import io.jacob.episodive.core.model.mapper.toIntSeconds
import io.jacob.episodive.core.model.mapper.toRelativeDate
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * 재생 중인 에피소드 행의 강조 배경 (원본 줄 436의 #2C2320).
 *
 * 행의 제목·부제는 테마 색(onSurface/onSurfaceVariant)이라 배경만 다크 값으로 고정하면
 * 라이트 테마에서 검은 배경 위 검은 글씨가 된다. 다크 스킴에서 #24201D 로 원본과 거의
 * 같은 토큰을 쓰고, 라이트에서는 그 테마의 밝은 표면색이 오게 한다.
 */
private val PlayingRowBackground: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh

/** 재생 중 행의 강조 배경이 행 바깥으로 번지는 폭과 모서리 반경. */
private val PlayingRowBleed = 8.dp
private val PlayingRowCornerRadius = 12.dp

/**
 * 강조 배경 테두리 표시.
 *
 * 테두리 전체는 항상 [PlayingRowIndicatorBaseAlpha] 로 은은하게 깔리고, 그 위를
 * [PlayingRowIndicatorSweep] 만큼의 구간 [PlayingRowIndicatorCount] 개가 둘레를
 * 균등하게 나눈 자리에서 꼬리에서 머리로 진해지며 함께 돈다.
 */
private val PlayingRowIndicatorWidth = 1.5.dp
private const val PlayingRowIndicatorSweep = 0.22f
private const val PlayingRowIndicatorDurationMs = 2600
private const val PlayingRowIndicatorBaseAlpha = 0.16f
private const val PlayingRowIndicatorHeadAlpha = 0.58f

/** 둘레를 도는 구간 수 — 둘레를 균등하게 나눈 자리에서 함께 돈다. */
private const val PlayingRowIndicatorCount = 2

/** 도는 구간을 몇 토막으로 나눠 그릴지 — 토막마다 알파를 올려 꼬리가 자연스럽게 흐려진다. */
private const val PlayingRowIndicatorSteps = 8

/**
 * 강조 배경 테두리를 한 방향으로 계속 도는 무한 표시.
 *
 * 청취 진행률이 아니라 "이 항목이 지금 돌고 있다"는 표시라 [phase] 는 항상 0→1 을 반복한다.
 * 경로와 측정기는 크기가 바뀔 때만 다시 만들고, 매 프레임에는 잘라낼 구간만 계산한다.
 */
private fun Modifier.playingRowIndicator(
    phase: State<Float>,
    color: Color,
): Modifier = drawWithCache {
    // 토막을 이어 붙이므로 끝을 둥글리지 않는다. 둥글리면 이음매마다 혹이 생긴다.
    val stroke = Stroke(width = PlayingRowIndicatorWidth.toPx(), cap = StrokeCap.Butt)
    val inset = stroke.width / 2f
    val radius = (PlayingRowCornerRadius.toPx() - inset).coerceAtLeast(0f)

    val outline = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(inset, inset, size.width - inset, size.height - inset),
                cornerRadius = CornerRadius(radius),
            )
        )
    }
    val measure = PathMeasure().apply { setPath(outline, forceClosed = true) }
    val total = measure.length
    val segment = Path()

    onDrawWithContent {
        drawContent()
        if (total <= 0f) return@onDrawWithContent

        drawPath(
            path = outline,
            color = color.copy(alpha = PlayingRowIndicatorBaseAlpha),
            style = stroke,
        )

        val sweep = total * PlayingRowIndicatorSweep

        repeat(PlayingRowIndicatorCount) { index ->
            val head = (phase.value + index.toFloat() / PlayingRowIndicatorCount) * total

            repeat(PlayingRowIndicatorSteps) { step ->
                val from = head + sweep * step / PlayingRowIndicatorSteps
                val to = head + sweep * (step + 1) / PlayingRowIndicatorSteps

                segment.reset()
                measure.appendSegment(segment, from, to, total)

                val ratio = (step + 1f) / PlayingRowIndicatorSteps
                val alpha = PlayingRowIndicatorBaseAlpha +
                        (PlayingRowIndicatorHeadAlpha - PlayingRowIndicatorBaseAlpha) * ratio

                drawPath(path = segment, color = color.copy(alpha = alpha), style = stroke)
            }
        }
    }
}

/** 시작점이 한 바퀴를 넘어가면 앞쪽으로 돌아와 이어 붙인다. */
private fun PathMeasure.appendSegment(destination: Path, from: Float, to: Float, total: Float) {
    val start = from % total
    val end = start + (to - from)

    if (end <= total) {
        getSegment(start, end, destination, startWithMoveTo = true)
    } else {
        getSegment(start, total, destination, startWithMoveTo = true)
        getSegment(0f, end - total, destination, startWithMoveTo = true)
    }
}

/**
 * 레이아웃 크기는 그대로 두고, 노드 자체만 사방 [bleed] 만큼 넓혀 그 자리에 배경·리플이
 * 그려지게 한다. 안쪽에 같은 값의 padding 을 물려 내용 위치는 원래대로 돌린다.
 *
 * 강조 배경을 drawBehind 로 바깥에 그리면 리플은 행 크기 그대로라 눌렀을 때 밝아지는
 * 사각형과 강조 영역이 어긋나 보인다.
 */
private fun Modifier.bleedingRowSurface(bleed: Dp) = layout { measurable, constraints ->
    val extra = bleed.roundToPx() * 2
    val placeable = measurable.measure(constraints.offset(horizontal = extra, vertical = extra))

    layout(placeable.width - extra, placeable.height - extra) {
        placeable.place(-extra / 2, -extra / 2)
    }
}

/** 클립 화면 인용문/출처는 배경 이미지 위에 얹히므로 테마 색이 아닌 고정 흰 계열을 쓴다 (원본 줄 461~462). */
private val ClipQuoteSourceColor = Color.White.copy(alpha = 0.82f)

// 클립 카드 위 스크림. 화면 배경이 클립 커버 색을 따라 바뀌므로, 특정 색조를 덧칠하지 않고
// 중립 검정으로만 눌러 커버 색이 그대로 살아 있게 한다 (원본 줄 455).
private val ClipCardScrimColor = Color.Black.copy(alpha = 0.52f)

@Composable
fun EpisodesSection(
    modifier: Modifier = Modifier,
    title: String,
    episodes: List<Episode>,
    onEpisodeClick: (Episode) -> Unit,
    onToggleLikedEpisode: (Episode) -> Unit,
    onToggleSavedEpisode: (Episode) -> Unit = {},
    onMore: (() -> Unit)? = null,
) {
    val dimension = LocalDimensionTheme.current

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimension.screenPadding),
            verticalArrangement = Arrangement.spacedBy(dimension.listItemSpacing)
        ) {
            episodes.forEach { episode ->
                EpisodeItem(
                    episode = episode,
                    onClick = { onEpisodeClick(episode) },
                    onToggleLiked = { onToggleLikedEpisode(episode) },
                    onToggleSaved = { onToggleSavedEpisode(episode) },
                )
            }
        }
    }
}

/**
 * [EpisodesSection] 로딩 자리. 실제 섹션이 LazyRow가 아니라 세로 Column이므로 카드를
 * 가로로 늘어놓지 않고 [count]개를 세로로 쌓는다.
 */
@Composable
fun EpisodesSectionSkeleton(
    modifier: Modifier = Modifier,
    count: Int = 3,
    hasAction: Boolean = false,
) {
    val dimension = LocalDimensionTheme.current

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeaderSkeleton(hasAction = hasAction)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimension.screenPadding),
            verticalArrangement = Arrangement.spacedBy(dimension.listItemSpacing)
        ) {
            repeat(count) {
                EpisodeItemSkeleton()
            }
        }
    }
}

fun LazyListScope.episodeItems(
    itemModifier: Modifier = Modifier,
    episodes: List<Episode>,
    playingIndex: Int,
    onEpisodeClick: (Episode) -> Unit,
    onToggleLikedEpisode: (Episode) -> Unit,
    onToggleSavedEpisode: (Episode) -> Unit = {},
) {
    itemsIndexed(
        items = episodes,
        key = { _, episode ->
            episode.id
        }
    ) { index, episode ->
        EpisodeItem(
            modifier = itemModifier,
            episode = episode,
            // 재생목록은 큐에서의 현재 위치를 알고 있으므로 그것으로 직접 표시한다.
            isPlaying = index == playingIndex,
            onClick = { onEpisodeClick(episode) },
            onToggleLiked = { onToggleLikedEpisode(episode) },
            onToggleSaved = { onToggleSavedEpisode(episode) },
        )
    }
}

@Composable
fun EpisodeItem(
    modifier: Modifier = Modifier,
    episode: Episode,
    // 지금 재생 중인지. 기본값은 앱이 제공하는 현재 재생 에피소드와 비교해 스스로 판단하므로
    // 호출부가 따로 넘기지 않아도 목록 어디서든 재생 중인 한 항목만 강조된다.
    isPlaying: Boolean = episode.id == LocalNowPlayingEpisodeId.current,
    onClick: () -> Unit,
    onToggleLiked: () -> Unit,
    onToggleSaved: () -> Unit = {},
) {
    val dimension = LocalDimensionTheme.current
    val indicatorColor = MaterialTheme.colorScheme.primary

    // 재생 중 표시는 강조 배경 테두리를 도는 무한 프로그레스다. 청취 진행률이 아니라
    // "지금 이 항목이 돌고 있다"만 나타내므로 한 바퀴를 일정한 속도로 계속 돈다.
    val indicatorPhase = if (isPlaying) {
        rememberInfiniteTransition(label = "playingRow").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = PlayingRowIndicatorDurationMs,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Restart,
            ),
            label = "playingRowIndicator",
        )
    } else {
        null
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            // 강조 배경과 클릭 리플은 행보다 사방 8dp 넓은 둥근 영역에 함께 그린다.
            // 레이아웃 크기는 그대로라 재생 중인 행만 폭·높이가 달라지지 않고, 리플이
            // 눌린 영역도 강조 배경과 정확히 같은 모양이 된다.
            .bleedingRowSurface(PlayingRowBleed)
            .then(
                // clip 바깥에 둬야 테두리 획이 절반으로 잘리지 않는다.
                if (indicatorPhase != null) {
                    Modifier.playingRowIndicator(indicatorPhase, indicatorColor)
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(PlayingRowCornerRadius))
            .then(
                // 강조 배경은 현재 재생 항목에만 (원본 줄 436).
                if (isPlaying) Modifier.background(PlayingRowBackground) else Modifier
            )
            .clickable { onClick() }
            .padding(PlayingRowBleed),
        // 원본의 리스트 행 gap 은 12 다 (원본 줄 247·297·350).
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StateImage(
            modifier = Modifier
                .size(dimension.thumbnailSmall)
                .clip(MaterialTheme.shapes.medium),
            imageUrl = episode.coverUrl,
            contentDescription = episode.title,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            val subTitle = "%s • %s".format(
                episode.datePublished.toHumanReadable(),
                episode.feedTitle ?: episode.duration?.toHumanReadable() ?: ""
            ).trim()

            Text(
                text = subTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // 재생 버튼은 두지 않는다 — 행 전체가 이미 재생 동작이고, 재생 중인 항목은
        // 강조 배경 테두리를 도는 프로그레스로 구분된다.
        EpisodiveIconToggleButton(
            modifier = Modifier.size(19.dp),
            checked = episode.isLiked,
            onCheckedChange = { onToggleLiked() },
            colors = IconButtonDefaults.iconToggleButtonColors(
                checkedContainerColor = Color.Transparent,
                checkedContentColor = MaterialTheme.colorScheme.primary,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            icon = {
                MorphIcon(
                    modifier = Modifier.size(19.dp),
                    imageVector = if (episode.isLiked) EpisodiveIcons.LikeFilled else EpisodiveIcons.Like,
                    contentDescription = if (episode.isLiked) "Unlike" else "Like",
                )
            },
        )
    }
}

/**
 * [EpisodeItem] 로딩 자리. 재생 중 강조 배경·테두리는 로딩 상태에 의미가 없으므로 만들지
 * 않고, 썸네일·텍스트·좋아요 자리만 같은 치수로 채운다.
 */
@Composable
fun EpisodeItemSkeleton(modifier: Modifier = Modifier) {
    val dimension = LocalDimensionTheme.current

    Row(
        modifier = modifier.fillMaxWidth(),
        // EpisodeItem 행의 gap(12dp)과 동일.
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonCover(size = dimension.thumbnailSmall)

        // 줄 사이에 간격을 두지 않는다. SkeletonLine 은 이미 style 의 줄 높이를 통째로
        // 차지하고 그 안에 잉크만 얇게 칠하므로, 쌓기만 해도 실제 텍스트와 같은 리듬이 난다.
        // 여기에 spacedBy 를 걸면 세 줄 사이에 두 번 붙어 실제보다 8dp 길어지고, 그만큼
        // 로딩에서 콘텐츠로 바뀔 때 아래가 위로 당겨진다 — 실제 EpisodeItem 의 높이는
        // 커버(thumbnailSmall)가 정하는데, 제목이 두 줄로 접혀도 한 Text 안이라 줄 사이에
        // 간격이 없어 텍스트가 커버를 넘지 않기 때문이다.
        Column(modifier = Modifier.weight(1f)) {
            // 제목은 최대 2줄이라 항상 2줄을 그린다.
            SkeletonLine(style = MaterialTheme.typography.labelLarge, widthFraction = 0.92f)
            SkeletonLine(style = MaterialTheme.typography.labelLarge, widthFraction = 0.55f)
            SkeletonLine(style = MaterialTheme.typography.bodySmall, widthFraction = 0.45f)
        }

        // 좋아요 토글 버튼과 같은 19dp 원.
        SkeletonBox(
            modifier = Modifier.size(19.dp),
            shape = CircleShape,
        )
    }
}

@Stable
@Composable
fun PlayingEpisodesSection(
    modifier: Modifier = Modifier,
    playingEpisodes: List<Episode>,
    onEpisodeClick: (Episode) -> Unit,
) {
    val dimension = LocalDimensionTheme.current

    SubSectionHeader(
        modifier = modifier,
        title = stringResource(R.string.core_ui_continue),
    ) {
        val lazyListState = rememberLazyListState()
        val flingBehavior = rememberSnapFlingBehavior(
            lazyListState = lazyListState,
            snapPosition = SnapPosition.Start,
        )

        val firstEpisodeId = playingEpisodes.firstOrNull()?.id

        LaunchedEffect(firstEpisodeId) {
            if (firstEpisodeId != null) {
                lazyListState.animateScrollToItem(0)
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            state = lazyListState,
            flingBehavior = flingBehavior,
            horizontalArrangement = Arrangement.spacedBy(dimension.carouselSpacing),
            contentPadding = PaddingValues(horizontal = dimension.screenPadding),
            // 짧은 캐러셀은 빠른 스와이프의 드래그 구간만으로도 가장자리 stretch 가 쌓여
            // 릴리즈 시 움찔거림이 생기므로 overscroll 을 사용하지 않는다.
            overscrollEffect = null,
        ) {
            playingEpisodes(
                playingEpisodes = playingEpisodes,
                onEpisodeClick = onEpisodeClick
            )
        }
    }
}

fun LazyListScope.playingEpisodes(
    itemModifier: Modifier = Modifier,
    playingEpisodes: LazyPagingItems<Episode>,
    onEpisodeClick: (Episode) -> Unit,
) = items(
    count = playingEpisodes.itemCount,
    key = { playingEpisodes[it]?.id ?: it },
    itemContent = { index ->
        playingEpisodes[index]?.let { playedEpisode ->
            PlayingEpisodeItem(
                modifier = itemModifier.animateItem(),
                playedEpisode = playedEpisode,
                onClick = { onEpisodeClick(playedEpisode) }
            )
        }
    }
)

fun LazyListScope.playingEpisodes(
    itemModifier: Modifier = Modifier,
    playingEpisodes: List<Episode>,
    onEpisodeClick: (Episode) -> Unit,
) = items(
    items = playingEpisodes,
    key = { it.id },
    itemContent = { playedEpisode ->
        PlayingEpisodeItem(
            modifier = itemModifier.animateItem(),
            playedEpisode = playedEpisode,
            onClick = { onEpisodeClick(playedEpisode) }
        )
    }
)

@Composable
fun PlayingEpisodeItem(
    modifier: Modifier = Modifier,
    playedEpisode: Episode,
    onClick: () -> Unit,
) {
    val dimension = LocalDimensionTheme.current

    Surface(
        modifier = modifier
            .size(width = 192.dp, height = 84.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                    .clip(EpisodiveShapes.coverForSize(68)),
                imageUrl = playedEpisode.coverUrl,
                contentDescription = playedEpisode.title,
            )

            Column(
                modifier = Modifier
                    .width(98.dp)
            ) {
                Text(
                    text = playedEpisode.playedAt?.toRelativeDate() ?: playedEpisode.feedTitle
                    ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = playedEpisode.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimension.progressThicknessThin)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    gapSize = (-4).dp,
                    drawStopIndicator = {},
                    progress = {
                        val duration = playedEpisode.duration?.toIntSeconds()
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

@Composable
fun PlayedEpisodeItem(
    modifier: Modifier = Modifier,
    playedEpisode: Episode,
    showMoreInfo: Boolean = true,
    onClick: () -> Unit,
) {
    val dimension = LocalDimensionTheme.current

    Row(
        modifier = modifier
            .clickable { onClick() },
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        StateImage(
            modifier = Modifier
                .size(dimension.thumbnailMedium)
                .clip(EpisodiveShapes.miniPlayer),
            imageUrl = playedEpisode.coverUrl,
            contentDescription = playedEpisode.title,
        )

        Column(
            // weight 가 없으면 열 폭이 가장 긴 자식(제목)에 맞춰져, 제목이 짧은 항목만
            // 진행바가 뭉텅 짧아진다. 원본은 진행바가 항상 텍스트 열 전체 폭이다 (원본 줄 496).
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = playedEpisode.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            playedEpisode.feedTitle?.let { feedTitle ->
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = feedTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .weight(1f)
                        .height(dimension.progressThickness)
                        .clip(CircleShape),
                    color = if (playedEpisode.isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    gapSize = (-4).dp,
                    drawStopIndicator = {},
                    progress = { playedEpisode.progress },
                )

                Text(
                    text = if (playedEpisode.isCompleted) {
                        stringResource(R.string.core_ui_completed)
                    } else {
                        "${(playedEpisode.progress * 100).toInt()}%"
                    },
                    // 진행률 라벨은 굵기 지정이 없는 메타다 (원본 줄 496).
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // 완료 항목은 진행률 자리에 이미 "완료"가 찍히므로 여기서 반복하지 않는다.
            // 남길 내용이 없으면 줄 자체를 그리지 않아 빈 여백도 남지 않게 한다.
            val moreInfo = when {
                playedEpisode.isCompleted -> ""
                playedEpisode.remain != null ->
                    "${playedEpisode.remain?.toHumanReadable()} ${stringResource(R.string.core_ui_left)}"

                else -> ""
            }

            if (showMoreInfo && moreInfo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = moreInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * [PlayedEpisodeItem] 로딩 자리. 항목 폭은 실제와 마찬가지로 호출부가 [modifier]로
 * 정한다(가로 캐러셀에서는 250dp 고정, 세로 목록에서는 fillMaxWidth) — 여기서 폭을
 * 강제하면 둘 중 한 호출부는 어긋난다.
 */
@Composable
fun PlayedEpisodeItemSkeleton(modifier: Modifier = Modifier) {
    val dimension = LocalDimensionTheme.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        SkeletonCover(size = dimension.thumbnailMedium)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            SkeletonLine(style = MaterialTheme.typography.labelLarge, widthFraction = 0.8f)

            Spacer(modifier = Modifier.height(4.dp))
            SkeletonLine(style = MaterialTheme.typography.bodySmall, widthFraction = 0.5f)

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 진행률 바 자리.
                SkeletonBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(dimension.progressThickness),
                    shape = CircleShape,
                )

                // "42%"/"완료" 라벨 자리.
                SkeletonLine(
                    modifier = Modifier.width(28.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            // 남은 시간 안내 줄 — showMoreInfo가 true인 일반적인 경우를 기준으로 둔다.
            SkeletonLine(style = MaterialTheme.typography.bodySmall, widthFraction = 0.35f)
        }
    }
}

@Composable
fun EpisodeDetailItem(
    modifier: Modifier = Modifier,
    episode: Episode,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        StateImage(
            modifier = Modifier
                .size(200.dp)
                .clip(MaterialTheme.shapes.large),
            imageUrl = episode.coverUrl,
            contentDescription = episode.title,
        )

        Spacer(modifier = Modifier.height(8.dp))

        val subTitle = "%s • %s".format(
            episode.datePublished.toHumanReadable(),
            episode.duration?.toHumanReadable() ?: episode.feedTitle
        ).trim()

        Text(
            text = subTitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = episode.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        HtmlTextContainer(
            text = episode.description ?: "",
            enableLinks = false,
        ) {
            Text(
                text = it,
                maxLines = 4,
                minLines = 4,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * [EpisodeDetailItem] 로딩 자리. 설명은 실제와 같은 4줄 고정(minLines=maxLines=4)이라
 * 줄 수를 어림하지 않고 정확히 [EpisodeDetailDescriptionLines]개를 그린다 — 마지막 줄만
 * 짧게 줘 문단이 끝나는 자리처럼 보이게 한다.
 */
@Composable
fun EpisodeDetailItemSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(200.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 이 커버만 코너 사다리를 벗어난다 — 실제가 shapes.large 로 클립하므로 그대로 맞춘다.
        SkeletonCover(size = 200.dp, shape = MaterialTheme.shapes.large)

        Spacer(modifier = Modifier.height(8.dp))

        SkeletonLine(style = MaterialTheme.typography.bodySmall, widthFraction = 0.6f)
        SkeletonLine(style = MaterialTheme.typography.labelLarge, widthFraction = 0.85f)

        // 설명 4줄 — 한 Text 안의 여러 줄이라 줄 사이에 별도 간격을 더하지 않는다.
        Column {
            repeat(EpisodeDetailDescriptionLines) { index ->
                SkeletonLine(
                    style = MaterialTheme.typography.bodySmall,
                    widthFraction = if (index == EpisodeDetailDescriptionLines - 1) 0.5f else 1f,
                )
            }
        }
    }
}

private const val EpisodeDetailDescriptionLines = 4

/**
 * @param onTogglePlay 재생 버튼을 누른 결과. `true` 면 재생을, `false` 면 일시정지를 원한다는 뜻이다.
 * 토글 방향을 버리고 늘 재생으로 처리하면 일시정지가 곧바로 재생으로 되돌아온다.
 * @param onShare 주지 않으면 공유 버튼을 그리지 않는다. 공용 컴포넌트라 모든 화면이 공유를
 * 원한다고 전제하지 않는다 — 섹션 헤더의 `onMore` 가 쓰는 것과 같은 방식이다.
 */
@Composable
fun EpisodeClipItem(
    modifier: Modifier = Modifier,
    episode: Episode,
    isPlaying: Boolean,
    remaining: Duration,
    bandLevel: (band: Int) -> Float = ::waveIdleBandLevel,
    onClick: () -> Unit,
    onTogglePlay: (play: Boolean) -> Unit,
    onToggleLikedEpisode: () -> Unit,
    onShare: (() -> Unit)? = null,
    onDominantColorExtracted: ((Color) -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.extraLarge,
        onClick = onClick,
        color = Color.Transparent,
    ) {
        StateImage(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 20.dp),
            imageUrl = episode.coverUrl,
            contentDescription = episode.title,
            // 이건 카드 뒤에 깔리는 블러 배경이다. 커버가 없을 때 머리글자까지 얹으면
            // 화면 폭만 한 글자가 흐리게 번져 앞의 커버와 겹친다.
            title = null,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ClipCardScrimColor)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            // 디자인의 클립 카드는 텍스트가 좌측에 붙는다(원본 줄 460). 커버만 중앙이고
            // 나머지가 좌측이면 정렬 기준이 둘로 갈려 어색해진다.
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        ) {
            Card(
                // 고정 250dp 는 목업(344px)보다 넓은 실기기에서 카드 안에 작게 떠 보인다.
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
            ) {
                StateImage(
                    modifier = Modifier
                        .fillMaxSize(),
                    imageUrl = episode.coverUrl,
                    contentDescription = episode.title,
                    // 화면 배경 그라디언트를 이 커버 색으로 물들이기 위해 위로 올린다.
                    onDominantColorExtracted = onDominantColorExtracted,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 인용문 — 19/800 흰색 (원본 줄 461).
                Text(
                    text = episode.title,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )

                // 출처 — 12.5/rgba(255,255,255,.82) (원본 줄 462).
                episode.feedTitle?.let { feedTitle ->
                    Text(
                        text = feedTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = ClipQuoteSourceColor,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClipAnimationIconText(
                    text = remaining.toHumanReadable(),
                    isPlaying = isPlaying,
                    bandLevel = bandLevel,
                )

                Spacer(modifier = Modifier.weight(1f))

                if (onShare != null) {
                    EpisodiveIconButton(
                        onClick = onShare,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                        ),
                        icon = {
                            Icon(
                                imageVector = EpisodiveIcons.Share,
                                contentDescription = stringResource(R.string.core_ui_share),
                                tint = Color.White,
                            )
                        }
                    )
                }

                EpisodiveIconToggleButton(
                    checked = episode.isLiked,
                    onCheckedChange = { onToggleLikedEpisode() },
                    colors = IconButtonDefaults.iconToggleButtonColors(
                        checkedContainerColor = Color.Transparent,
                        checkedContentColor = MaterialTheme.colorScheme.primary,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                    ),
                    icon = {
                        MorphIcon(
                            imageVector = if (episode.isLiked) EpisodiveIcons.LikeFilled else EpisodiveIcons.Like,
                            contentDescription = if (episode.isLiked) "Unlike" else "Like",
                        )
                    },
                )

                EpisodiveIconToggleButton(
                    checked = isPlaying,
                    onCheckedChange = onTogglePlay,
                    colors = IconButtonDefaults.iconToggleButtonColors(
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    icon = {
                        PlayPauseMorphIcon(
                            isPlaying = isPlaying,
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    },
                )
            }
        }
    }
}

/**
 * [EpisodeClipItem] 로딩 자리. 배경 블러 이미지·스크림·재생 컨트롤까지 그대로 옮기면
 * 로딩 상태에서 의미 없는 디테일만 늘어나므로, 카드 틀 + 커버 블록 + 인용문 줄로
 * 단순화한다. Surface의 색은 실제와 같은 Transparent를 그대로 둬 — 스켈레톤 블록은
 * 화면 배경 위에서만 대비가 생기고, 블록끼리 겹쳐서는 구분되지 않는다 — 패딩(24dp)과
 * 모서리(extraLarge)만 실제와 같게 맞춘다.
 */
@Composable
fun EpisodeClipItemSkeleton(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        ) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = MaterialTheme.shapes.extraLarge,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SkeletonLine(style = MaterialTheme.typography.titleMedium, widthFraction = 0.9f)
                SkeletonLine(style = MaterialTheme.typography.titleMedium, widthFraction = 0.7f)
                SkeletonLine(style = MaterialTheme.typography.bodySmall, widthFraction = 0.4f)
            }
        }
    }
}

@DevicePreviews
@Composable
private fun EpisodeItemPreview() {
    EpisodiveTheme {
        EpisodeItem(
            episode = episodeTestData.copy(likedAt = Instant.fromEpochSeconds(1234)),
            onClick = {},
            onToggleLiked = {},
            onToggleSaved = {},
        )
    }
}

@DevicePreviews
@Composable
private fun PlayingEpisodesPreview() {
    EpisodiveTheme {
        PlayingEpisodeItem(
            playedEpisode = episodeTestData,
            onClick = {},
        )
    }
}

@DevicePreviews
@Composable
private fun PlayedEpisodesPreview() {
    EpisodiveTheme {
        PlayedEpisodeItem(
            playedEpisode = episodeTestData,
            onClick = {},
        )
    }
}

@DevicePreviews
@Composable
private fun EpisodeDetailItemPreview() {
    EpisodiveTheme {
        EpisodeDetailItem(
            episode = episodeTestData,
            onClick = {},
        )
    }
}

@DevicePreviews
@Composable
private fun EpisodeClipItemPreview() {
    EpisodiveTheme {
        EpisodeClipItem(
            episode = episodeTestData.copy(
                clipStartTime = Instant.fromEpochSeconds(30),
                clipDuration = 60.seconds,
            ),
            isPlaying = true,
            remaining = 45.seconds,
            onClick = {},
            onTogglePlay = {},
            onToggleLikedEpisode = {},
        )
    }
}

@ThemePreviews
@Composable
private fun EpisodeItemSkeletonPreview() {
    EpisodiveTheme {
        val dimension = LocalDimensionTheme.current

        // 실제 / 스켈레톤 / 실제 순으로 쌓아 좌우 정렬선이 어긋나면 바로 드러나게 한다.
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = dimension.screenPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(dimension.listItemSpacing),
        ) {
            EpisodeItem(episode = episodeTestData, onClick = {}, onToggleLiked = {})
            EpisodeItemSkeleton()
            EpisodeItem(episode = episodeTestData, onClick = {}, onToggleLiked = {})
        }
    }
}

@ThemePreviews
@Composable
private fun EpisodesSectionSkeletonPreview() {
    EpisodiveTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            EpisodesSection(
                title = "Preview",
                episodes = episodeTestDataList.take(3),
                onEpisodeClick = {},
                onToggleLikedEpisode = {},
            )
            EpisodesSectionSkeleton()
        }
    }
}

@ThemePreviews
@Composable
private fun PlayedEpisodeItemSkeletonPreview() {
    EpisodiveTheme {
        // 실제 항목 폭은 호출부가 정하므로(가로 캐러셀 250dp) 여기서도 같은 폭으로 감싼다.
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .width(250.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PlayedEpisodeItem(playedEpisode = episodeTestData, onClick = {})
            PlayedEpisodeItemSkeleton()
            PlayedEpisodeItem(playedEpisode = episodeTestData, onClick = {})
        }
    }
}

@ThemePreviews
@Composable
private fun EpisodeDetailItemSkeletonPreview() {
    EpisodiveTheme {
        // 고정 200dp 폭 카드라 나란히 둬도 서로 폭을 다투지 않는다.
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            EpisodeDetailItem(episode = episodeTestData, onClick = {})
            EpisodeDetailItemSkeleton()
        }
    }
}

@ThemePreviews
@Composable
private fun EpisodeClipItemSkeletonPreview() {
    EpisodiveTheme {
        // 전체화면 카드라 위아래로 쌓아 비교한다 — 폭을 다투는 side-by-side는 둘 다 찌그러진다.
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(modifier = Modifier.width(220.dp).height(390.dp)) {
                EpisodeClipItem(
                    episode = episodeTestData,
                    isPlaying = true,
                    remaining = 45.seconds,
                    onClick = {},
                    onTogglePlay = {},
                    onToggleLikedEpisode = {},
                )
            }
            Box(modifier = Modifier.width(220.dp).height(390.dp)) {
                EpisodeClipItemSkeleton()
            }
        }
    }
}
