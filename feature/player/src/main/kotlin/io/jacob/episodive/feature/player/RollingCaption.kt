package io.jacob.episodive.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.jacob.episodive.core.designsystem.theme.EpisodiveShapes
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.caption.CaptionLine
import io.jacob.episodive.core.model.caption.LiveCaption
import io.jacob.episodive.core.testing.model.liveCaptionTestData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 롤링 자막 오버레이 — 앨범아트 전체를 자막 영역으로 쓴다. 새 줄은 아래에서 들어오고, 영역을
 * 넘치는 윗줄은 위로 밀려 올라가 사라진다. 줄 수 제한은 없다(영역 높이가 곧 한계다).
 *
 * [LiveCaption.isTranslating] 이면 커버를 위(원문)/아래(번역) 반씩 나눠 둘 다 같은 방식으로
 * 롤링한다. 나눌지를 번역 줄이 있는지로 정하지 않는 것은, 첫 번역이 도착하는 순간 원문 영역이
 * 반으로 줄며 튀기 때문이다.
 *
 * [caption] 이 null 이거나 원문 줄이 없으면 숨는다. 호출부는 이 컴포저블을 **분기 없이 늘
 * 부른다** — 조건에 따라 부르다 말면 [AnimatedVisibility] 가 컴포지션에서 통째로 빠져 페이드
 * 인/아웃이 일어나지 않는다. 숨는 동안(페이드아웃)에는 마지막으로 보인 자막을 그대로 그린다.
 */
@Composable
fun RollingCaptionOverlay(
    modifier: Modifier = Modifier,
    caption: LiveCaption?,
) {
    val visibleCaption = caption?.takeIf { it.lines.isNotEmpty() }
    var lastShown by remember { mutableStateOf<LiveCaption?>(null) }
    SideEffect { if (visibleCaption != null) lastShown = visibleCaption }

    AnimatedVisibility(
        modifier = modifier,
        visible = visibleCaption != null,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
    ) {
        val shown = visibleCaption ?: lastShown ?: return@AnimatedVisibility

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    // 글자가 커버 맨 위까지 차므로 위쪽도 충분히 어둡게 깐다 — 밝은 커버에서도
                    // 윗줄이 읽혀야 한다.
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.7f),
                        ),
                    ),
                    shape = EpisodiveShapes.playerCover,
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RollingCaptionLines(
                modifier = Modifier.weight(1f),
                episodeId = shown.episodeId,
                lines = shown.lines,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )

            if (shown.isTranslating) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = Color.White.copy(alpha = 0.2f),
                )

                RollingCaptionLines(
                    modifier = Modifier.weight(1f),
                    episodeId = shown.episodeId,
                    lines = shown.translations,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

/**
 * 롤링 영역 하나 — 부모가 정한 높이 안에, 줄([CaptionLine])마다 별도 [Text] 블록을 위→아래로
 * 쌓고 **맨 아래 줄을 영역 바닥에 붙인다.** 넘치는 윗줄은 clip 밖(위)으로 나간다. 한 Text 에
 * 이어붙인 뒤 앞을 잘라내는 방식은 줄바꿈 재계산으로 튀므로 쓰지 않는다.
 *
 * 지나온 길 — 둘 다 실기기에서 깨졌다:
 * - `LazyColumn(reverseLayout = true)` + `animateItem()`: 뷰포트를 벗어난 아이템을 Lazy 가 즉시
 *   dispose 하고 퇴장은 애니메이션하지 않아, 맨 윗줄이 밀려 올라가는 과정 없이 사라졌다.
 * - "콘텐츠 전체 높이가 늘면 애니메이션": 목록이 꽉 차면 presenter 가 맨 윗줄을 버리는 만큼 높이가
 *   빠져 증감이 상쇄되고, 새 줄이 제자리에서 툭 바뀌었다. 또 높이를 state 로 한 번 거쳐 오프셋에
 *   반영하느라 한 프레임 늦었다.
 *
 * 그래서 [Layout] 의 측정 단계에서 줄마다 높이를 재고, **이전과 지금 모두 있는 가장 최근 줄
 * (앵커)** 이 영역 바닥에서 얼마나 멀어졌는지로 이동량을 잡는다([RollingOffset]). 새 줄이
 * 붙거나 partial 이 한 줄 더 길어지면 앵커가 그만큼 위로 가므로 그 거리를 부드럽게 따라가고,
 * 맨 윗줄이 버려지는 것은 앵커와 바닥 사이 거리를 바꾸지 않으므로 움직임이 없다.
 */
@Composable
private fun RollingCaptionLines(
    modifier: Modifier = Modifier,
    episodeId: Long,
    lines: List<CaptionLine>,
    style: TextStyle,
    color: Color,
) {
    val scope = rememberCoroutineScope()
    // 줄 id 는 에피소드(세션)마다 0 부터 다시 매겨진다. 앞 에피소드의 페이드아웃 중에 다음
    // 에피소드 줄이 들어오면 같은 id 를 앵커로 잘못 잡으므로, 에피소드가 바뀌면 새로 시작한다.
    val rolling = remember(scope, episodeId) { RollingOffset(scope) }
    val lineSpacingPx = with(LocalDensity.current) { LineSpacing.roundToPx() }

    Layout(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .topEdgeFade(),
        content = {
            lines.forEach { line ->
                key(line.id) {
                    Text(
                        modifier = Modifier.layoutId(line.id),
                        text = line.text,
                        color = color,
                        style = style,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(
            minWidth = constraints.maxWidth,
            minHeight = 0,
            maxHeight = Constraints.Infinity,
        )
        val placeables = measurables.map { it.measure(childConstraints) }

        val tops = IntArray(placeables.size)
        var contentHeight = 0
        placeables.forEachIndexed { index, placeable ->
            if (index > 0) contentHeight += lineSpacingPx
            tops[index] = contentHeight
            contentHeight += placeable.height
        }

        // 줄 id → 그 줄 윗변이 콘텐츠 바닥에서 떨어진 거리(오래된 → 최신 순서 유지).
        val distances = LinkedHashMap<Long, Int>(measurables.size)
        measurables.forEachIndexed { index, measurable ->
            distances[measurable.layoutId as Long] = contentHeight - tops[index]
        }
        rolling.onMeasured(distances)

        val width = constraints.maxWidth
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else contentHeight
        layout(width, height) {
            // offset 은 여기(배치 단계)에서만 읽는다 — 애니메이션 프레임마다 재측정하지 않고
            // 재배치만 일어난다.
            val bottomAligned = height - contentHeight + rolling.offset.roundToInt()
            placeables.forEachIndexed { index, placeable ->
                placeable.place(0, bottomAligned + tops[index])
            }
        }
    }
}

/**
 * [RollingCaptionLines] 의 세로 이동량. 앵커 줄이 위로 `d` 만큼 올라갔으면 [offset] 을 `+d` 로
 * 두어 **이번 프레임에는 제자리에 그리고**, 그 뒤 0 으로 애니메이션해 위로 슬라이드시킨다.
 * 측정 중에 바로 값을 바꾸므로 한 프레임 늦는 일이 없다.
 */
internal class RollingOffset(private val scope: CoroutineScope) {
    var offset by mutableFloatStateOf(0f)
        private set

    private var previous: Map<Long, Int> = emptyMap()
    private var animation: Job? = null

    fun onMeasured(distances: Map<Long, Int>) {
        val anchor = distances.keys.lastOrNull { it in previous }
        val delta = anchor?.let { distances.getValue(it) - previous.getValue(it) }
        previous = distances

        if (delta == null) {
            // 처음 나타났거나 줄이 통째로 바뀌었다(구간 변경) — 따라갈 대상이 없으니 제자리에.
            animation?.cancel()
            offset = 0f
            return
        }
        if (delta == 0) return

        val start = Snapshot.withoutReadObservation { offset } + delta
        offset = start
        animation?.cancel()
        animation = scope.launch {
            animate(initialValue = start, targetValue = 0f, animationSpec = tween(250)) { value, _ ->
                offset = value
            }
        }
    }
}

private val LineSpacing = 4.dp

/** 위쪽 가장자리만 투명→불투명으로 페이드시켜, 위로 밀려 사라지는 줄이 뚝 끊기지 않게 한다. */
private fun Modifier.topEdgeFade(fadeHeight: Dp = 24.dp): Modifier = composed {
    val density = LocalDensity.current

    this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val fadeHeightPx = with(density) { fadeHeight.toPx() }
            val fadeRatio = (fadeHeightPx / size.height).coerceIn(0f, 1f)

            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    fadeRatio to Color.Black,
                    1f to Color.Black,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
}

@DevicePreviews
@Composable
private fun RollingCaptionOverlayTranslatedPreview() {
    EpisodiveTheme {
        RollingCaptionOverlay(caption = liveCaptionTestData)
    }
}

@DevicePreviews
@Composable
private fun RollingCaptionOverlayOriginalOnlyPreview() {
    EpisodiveTheme {
        RollingCaptionOverlay(
            caption = liveCaptionTestData.copy(translations = emptyList(), isTranslating = false),
        )
    }
}
