package io.jacob.episodive.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.jacob.episodive.core.designsystem.component.EpisodiveIconText
import io.jacob.episodive.core.designsystem.component.EpisodiveIconToggleButton
import io.jacob.episodive.core.designsystem.component.EpisodiveOutlinedButton
import io.jacob.episodive.core.designsystem.component.HtmlTextContainer
import io.jacob.episodive.core.designsystem.component.MorphIcon
import io.jacob.episodive.core.designsystem.component.SectionHeader
import io.jacob.episodive.core.designsystem.component.SectionHeaderSkeleton
import io.jacob.episodive.core.designsystem.component.SkeletonBox
import io.jacob.episodive.core.designsystem.component.SkeletonCover
import io.jacob.episodive.core.designsystem.component.SkeletonLine
import io.jacob.episodive.core.designsystem.component.StateImage
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveShapes
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.designsystem.tooling.ThemePreviews
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.coverUrl
import io.jacob.episodive.core.model.mapper.toHumanReadable
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.testing.model.podcastTestDataList

@Stable
@Composable
fun PodcastsSection(
    modifier: Modifier = Modifier,
    title: String,
    podcasts: List<Podcast>,
    subtitleProvider: ((Podcast) -> String)? = null,
    onMore: (() -> Unit)? = null,
    onPodcastClick: (Podcast) -> Unit = {},
) {
    SectionHeader(
        modifier = modifier,
        title = title,
        // 더 보기 어포던스는 onMore 유무로만 결정한다. 기본값을 빈 람다로 두면 갈 곳이 없는
        // 화면에도 버튼이 뜨고, 눌러도 아무 일이 없다.
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

        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            state = lazyListState,
            flingBehavior = flingBehavior,
            // 같은 화면의 채널·에피소드 섹션은 20dp 화면 패딩을 쓴다. 여기만 16dp 면
            // 한 화면 안에 좌측 정렬선이 두 개 생긴다 (원본 줄 242).
            horizontalArrangement = Arrangement.spacedBy(dimension.carouselSpacing),
            contentPadding = PaddingValues(horizontal = dimension.screenPadding),
            // 짧은 캐러셀은 빠른 스와이프의 드래그 구간만으로도 가장자리 stretch 가 쌓여
            // 릴리즈 시 움찔거림이 생기므로 overscroll 을 사용하지 않는다.
            overscrollEffect = null,
        ) {
            podcasts(
                podcasts = podcasts,
                subtitleProvider = subtitleProvider,
                onPodcastClick = onPodcastClick
            )
        }
    }
}

/** [PodcastsSection] 로딩 자리. 실제와 같은 헤더·캐러셀 여백을 써서 전환 시 레이아웃이 튀지 않는다. */
@Composable
fun PodcastsSectionSkeleton(
    modifier: Modifier = Modifier,
    count: Int = 3,
    hasAction: Boolean = false,
) {
    val dimension = LocalDimensionTheme.current

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        SectionHeaderSkeleton(hasAction = hasAction)

        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            // 아직 데이터가 없는 캐러셀이라 좌우로 끌리면 없는 콘텐츠를 만지는 것처럼
            // 보인다. 스크롤을 막아 "곧 채워질 자리"로만 읽히게 한다.
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(dimension.carouselSpacing),
            contentPadding = PaddingValues(horizontal = dimension.screenPadding),
        ) {
            items(count) {
                PodcastItemSkeleton()
            }
        }
    }
}

fun LazyListScope.podcasts(
    itemModifier: Modifier = Modifier,
    podcasts: List<Podcast>,
    subtitleProvider: ((Podcast) -> String)? = null,
    onPodcastClick: (Podcast) -> Unit,
) = items(
    items = podcasts,
    key = { it.id }
) { podcast ->
    PodcastItem(
        modifier = itemModifier,
        podcast = podcast,
        subtitle = subtitleProvider?.invoke(podcast),
        onClick = { onPodcastClick(podcast) }
    )
}

@Composable
fun PodcastItem(
    modifier: Modifier = Modifier,
    podcast: Podcast,
    subtitle: String? = null,
    onClick: () -> Unit = {},
) {
    val textSectionMinHeight = rememberPodcastTextSectionMinHeight()
    val subtitleText = subtitle
        ?: "${podcast.episodeCount} ${stringResource(R.string.core_ui_episodes)}"

    Column(
        modifier = Modifier
            // 기본 폭은 캐러셀 셀(118dp)이다. 그리드처럼 폭을 바깥에서 정하는 호출부가
            // weight/fillMaxWidth 로 덮어쓸 수 있도록 modifier 보다 앞에 둔다.
            .width(LocalDimensionTheme.current.coverCarousel)
            .then(modifier)
            .clickable { onClick() },
    ) {
        StateImage(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.large),
            imageUrl = podcast.coverUrl,
            contentDescription = podcast.title,
        )

        Spacer(modifier = Modifier.height(CoverTitleSpacing))

        Column(
            modifier = Modifier.heightIn(min = textSectionMinHeight),
        ) {
            Text(
                text = podcast.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(TextSectionSpacing))

            Text(
                text = subtitleText,
                // 원본의 11~12px 메타는 굵기 지정이 없다(=400). labelSmall 은 700 이라
                // 보조 정보가 제목만큼 굵어져 위계가 뭉개진다 (원본 줄 199·243).
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** [PodcastItem] 로딩 자리. 커버 비율·텍스트 자리 상수를 그대로 참조해 전환 시 크기가 안 튄다. */
@Composable
fun PodcastItemSkeleton(
    modifier: Modifier = Modifier,
) {
    val textSectionMinHeight = rememberPodcastTextSectionMinHeight()

    Column(
        modifier = Modifier
            // 실제 PodcastItem과 동일하게 modifier보다 앞에 둬서, 그리드처럼 폭을
            // 바깥에서 정하는 호출부가 weight/fillMaxWidth로 덮어쓸 수 있게 한다.
            .width(LocalDimensionTheme.current.coverCarousel)
            .then(modifier),
    ) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = MaterialTheme.shapes.large,
        )

        Spacer(modifier = Modifier.height(CoverTitleSpacing))

        Column(
            modifier = Modifier.heightIn(min = textSectionMinHeight),
        ) {
            SkeletonLine(
                style = MaterialTheme.typography.labelMedium,
                widthFraction = 0.85f,
            )

            Spacer(modifier = Modifier.height(TextSectionSpacing))

            SkeletonLine(
                style = MaterialTheme.typography.bodySmall,
                widthFraction = 0.55f,
            )
        }
    }
}

private val CoverTitleSpacing = 9.dp
private val TextSectionSpacing = 4.dp

@Composable
private fun rememberPodcastTextSectionMinHeight(): Dp {
    val density = LocalDensity.current
    val typography = MaterialTheme.typography
    return remember(density, typography) {
        with(density) {
            typography.labelMedium.lineHeight.toDp() +
                TextSectionSpacing +
                typography.bodySmall.lineHeight.toDp()
        }
    }
}

private val DetailItemCoverSize = 96.dp

/**
 * 제목 줄과 메타 줄 사이 간격은 이 버튼 지름이 정한다 — 제목 Row 의 높이가 둘 중 큰 쪽,
 * 즉 버튼 높이로 잡히기 때문이다. 기본값 46dp 로는 제목 아래 빈 공간이 그만큼 벌어진다.
 */
private val DetailItemFollowButtonSize = 34.dp
private val DetailItemFollowIconSize = 16.dp

@Composable
fun PodcastDetailItem(
    modifier: Modifier = Modifier,
    podcast: Podcast,
    isFollowed: Boolean = podcast.isFollowed,
    onClick: () -> Unit = {},
    onToggleFollowed: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick),
    ) {
        StateImage(
            modifier = Modifier
                .size(DetailItemCoverSize)
                .clip(EpisodiveShapes.coverForSize(96)),
            imageUrl = podcast.coverUrl,
            contentDescription = podcast.title,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f),
                    text = podcast.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                EpisodiveIconToggleButton(
                    checked = isFollowed,
                    onCheckedChange = { onToggleFollowed() },
                    size = DetailItemFollowButtonSize,
                    icon = {
                        MorphIcon(
                            modifier = Modifier.size(DetailItemFollowIconSize),
                            imageVector = if (isFollowed) EpisodiveIcons.PersonRemove else EpisodiveIcons.PersonAdd,
                            contentDescription = podcast.title,
                        )
                    },
                )
            }

            FlowRow(
                modifier = Modifier,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                podcast.ownerName.ifEmpty { podcast.author }.let { owner ->
                    if (owner.isNotEmpty()) {
                        EpisodiveIconText(
                            icon = {
                                Icon(
                                    imageVector = EpisodiveIcons.Owner,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp),
                                )
                            },
                            text = {
                                Text(
                                    text = podcast.ownerName.ifEmpty { podcast.author },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        )
                    }
                }

                EpisodiveIconText(
                    icon = {
                        Icon(
                            imageVector = EpisodiveIcons.PublicationDate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp),
                        )
                    },
                    text = {
                        Text(
                            text = (podcast.newestItemPublishTime
                                ?: podcast.lastUpdateTime).toHumanReadable(),
                            maxLines = 1,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                )

                EpisodiveIconText(
                    icon = {
                        Icon(
                            imageVector = EpisodiveIcons.ListNumbered,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp),
                        )
                    },
                    text = {
                        Text(
                            text = "${podcast.episodeCount} ${stringResource(R.string.core_ui_episodes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HtmlTextContainer(
                text = podcast.description,
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
}

/**
 * [PodcastDetailItem] 로딩 자리.
 *
 * 설명은 실제와 같은 4줄 고정이다 — 줄 수가 다르면 데이터가 채워질 때 아래 레이아웃이
 * 밀린다. 마지막 줄만 폭을 줄여 문단이 끝나는 자리처럼 보이게 한다.
 */
@Composable
fun PodcastDetailItemSkeleton(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
    ) {
        SkeletonCover(size = DetailItemCoverSize)

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SkeletonLine(
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    widthFraction = 0.6f,
                )

                SkeletonBox(
                    modifier = Modifier.size(DetailItemFollowButtonSize),
                    shape = EpisodiveShapes.pill,
                )
            }

            FlowRow(
                modifier = Modifier,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 소유자·발행일·에피소드 수 세 자리. 실제 EpisodiveIconText와 같은 12dp
                // 아이콘 + 6dp 간격 리듬을 그대로 쓴다.
                DetailItemMetaWidths.forEach { width ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SkeletonBox(modifier = Modifier.size(12.dp))

                        SkeletonLine(
                            modifier = Modifier.width(width),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column {
                repeat(DetailItemDescriptionLines) { index ->
                    SkeletonLine(
                        style = MaterialTheme.typography.bodySmall,
                        widthFraction = if (index == DetailItemDescriptionLines - 1) 0.6f else 1f,
                    )
                }
            }
        }
    }
}

private val DetailItemMetaWidths = listOf(64.dp, 56.dp, 72.dp)
private const val DetailItemDescriptionLines = 4

@Composable
fun PodcastSimpleItem(
    modifier: Modifier = Modifier,
    podcast: Podcast,
    onClick: () -> Unit,
    onToggleFollowed: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StateImage(
            modifier = Modifier
                .size(50.dp)
                .clip(EpisodiveShapes.field),
            imageUrl = podcast.coverUrl,
            contentDescription = podcast.title,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = podcast.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = podcast.ownerName.ifEmpty { podcast.author },
                // 저자명도 메타라 굵기 500 이 맞다 (원본 줄 326).
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        EpisodiveOutlinedButton(
            onClick = onToggleFollowed,
            shape = EpisodiveShapes.miniPlayer,
            contentPadding = PaddingValues(horizontal = 15.dp, vertical = 7.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (podcast.isFollowed) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            ),
        ) {
            Text(
                text = stringResource(
                    if (podcast.isFollowed) R.string.core_ui_unfollow
                    else R.string.core_ui_follow
                ),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@DevicePreviews
@Composable
private fun PodcastItemPreview() {
    EpisodiveTheme {
        PodcastItem(
            podcast = podcastTestData,
        )
    }
}

@DevicePreviews
@Composable
private fun PodcastWithAuthorPreview() {
    EpisodiveTheme {
        PodcastItem(
            podcast = podcastTestData,
            subtitle = podcastTestData.ownerName.ifEmpty { podcastTestData.author },
        )
    }
}

@DevicePreviews
@Composable
private fun PodcastDetailItemPreview() {
    EpisodiveTheme {
        PodcastDetailItem(
            podcast = podcastTestData,
        )
    }
}

@DevicePreviews
@Composable
private fun PodcastSimpleItemPreview() {
    EpisodiveTheme {
        PodcastSimpleItem(
            podcast = podcastTestData,
            onClick = {},
            onToggleFollowed = {},
        )
    }
}

@ThemePreviews
@Composable
private fun PodcastItemSkeletonPreview() {
    EpisodiveTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PodcastItem(podcast = podcastTestData)
            PodcastItemSkeleton()
        }
    }
}

// PodcastsSection·PodcastDetailItem은 폭을 스스로 채우는 컴포넌트라 PodcastItem처럼
// Row로 나란히 두면 서로 폭을 다투다 찌그러진다. 위아래로 쌓아 비교한다.

@ThemePreviews
@Composable
private fun PodcastsSectionSkeletonPreview() {
    EpisodiveTheme {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PodcastsSection(
                title = "인기 팟캐스트",
                podcasts = podcastTestDataList.take(3),
            )
            PodcastsSectionSkeleton()
        }
    }
}

@ThemePreviews
@Composable
private fun PodcastDetailItemSkeletonPreview() {
    EpisodiveTheme {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PodcastDetailItem(podcast = podcastTestData)
            PodcastDetailItemSkeleton()
        }
    }
}
