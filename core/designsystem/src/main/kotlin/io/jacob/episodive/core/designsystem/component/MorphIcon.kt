package io.jacob.episodive.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.ThemePreviews

/** 모양이 바뀌는 데 걸리는 시간. */
private const val MorphDurationMs = 250

/** 아이콘 좌표계. [io.jacob.episodive.core.designsystem.icon.tabler] 아이콘과 같은 24 뷰포트다. */
private const val MorphViewport = 24f

private val MorphDefaultIconSize = 24.dp

/**
 * 재생 ↔ 일시정지를 모양 그대로 이어 바꾸는 아이콘.
 *
 * 두 [androidx.compose.ui.graphics.vector.ImageVector] 는 경로 수(1 ↔ 2)도 명령 구성도 달라
 * 경로끼리 보간할 수 없다. 그래서 둘을 같은 뼈대로 다시 그린다 — 삼각형을 세로로 잘라 두
 * 사변형으로 보고, 왼쪽 조각은 왼쪽 막대로, 오른쪽 조각은 오른쪽 막대로 꼭짓점째 옮긴다.
 *
 * 모서리 둥글기는 다각형을 채운 뒤 같은 모양을 둥근 이음 선으로 한 번 더 그어 만든다. 선
 * 굵기의 절반이 곧 모서리 반지름이고 그만큼 바깥으로 부푼다. 그래서 꼭짓점은 원본 윤곽에서
 * 그 반지름만큼 안으로 들인 값이다. 이렇게 하면 양 끝(0·1)에서 원본 Tabler 아이콘과 윤곽이
 * 정확히 같다 — 재생은 반지름 1, 일시정지는 반지름 2 이고 그 사이에서 굵기도 함께 옮긴다.
 *
 * 반드시 **같은 컴포지션 자리에 머물러야** 움직인다. `if (isPlaying) A else B` 처럼 갈래를
 * 나눠 부르면 상태가 바뀔 때마다 새로 붙어 처음부터 목표값에 서 있으므로 모양이 툭 바뀐다.
 *
 * @param isPlaying 참이면 일시정지 모양(누르면 멈춘다), 거짓이면 재생 모양.
 */
@Composable
fun PlayPauseMorphIcon(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    val progress by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(MorphDurationMs, easing = FastOutSlowInEasing),
        label = "PlayPauseMorph",
    )
    val path = remember { Path() }

    // 기존 Icon 과 같은 설명을 단다. 테스트와 스크린리더가 이 문자열로 버튼을 찾는다.
    Canvas(
        modifier = modifier
            .semantics {
                contentDescription = if (isPlaying) "Pause" else "Play"
                role = Role.Image
            }
            .size(MorphDefaultIconSize)
    ) {
        // progress 는 그리기 단계에서만 읽는다 — 애니메이션 동안 재구성 없이 다시 그리기만 돈다.
        drawPlayPause(progress = progress, color = tint, path = path)
    }
}

/*
 * 꼭짓점 표. 각 줄의 네 점은 같은 순서(시계 방향)로 짝지어져 progress 에 따라 옮겨간다.
 *
 * 재생: 원본 삼각형(반지름 1 모서리)을 1 만큼 들인 (7,4)-(20,12)-(7,20) 을 x=13.5 에서 잘랐다.
 * 자른 자리의 위아래 점은 삼각형 빗변 위라, 조각 둘을 각각 부풀려도 이음매가 원본 빗변을 넘지
 * 않는다. 오른쪽 조각은 꼭짓점이 셋이라 끝점(20,12)을 두 번 적어 네 점을 맞춘다.
 *
 * 일시정지: 원본 막대 x∈[5,11]·[13,19], y∈[4,20](반지름 2)를 2 만큼 들였다.
 */
private val PlayLeft = floatArrayOf(7f, 4f, 13.5f, 8f, 13.5f, 16f, 7f, 20f)
private val PlayRight = floatArrayOf(13.5f, 8f, 20f, 12f, 20f, 12f, 13.5f, 16f)
private val PauseLeft = floatArrayOf(7f, 6f, 9f, 6f, 9f, 18f, 7f, 18f)
private val PauseRight = floatArrayOf(15f, 6f, 17f, 6f, 17f, 18f, 15f, 18f)

/** 모서리 반지름의 두 배. 재생 1, 일시정지 2. */
private const val PlayStrokeWidth = 2f
private const val PauseStrokeWidth = 4f

private fun DrawScope.drawPlayPause(progress: Float, color: Color, path: Path) {
    val scale = size.minDimension / MorphViewport
    val origin = Offset(
        x = (size.width - MorphViewport * scale) / 2f,
        y = (size.height - MorphViewport * scale) / 2f,
    )

    path.rewind()
    path.addMorphedQuad(PlayLeft, PauseLeft, progress, scale)
    path.addMorphedQuad(PlayRight, PauseRight, progress, scale)

    translate(origin.x, origin.y) {
        drawPath(path = path, color = color)
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = lerp(PlayStrokeWidth, PauseStrokeWidth, progress) * scale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

private fun Path.addMorphedQuad(from: FloatArray, to: FloatArray, progress: Float, scale: Float) {
    for (i in 0 until 4) {
        val x = lerp(from[i * 2], to[i * 2], progress) * scale
        val y = lerp(from[i * 2 + 1], to[i * 2 + 1], progress) * scale
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/**
 * [imageVector] 가 바뀌면 이전 모양에서 새 모양으로 이어 바꾸는 아이콘. `Icon` 자리에 그대로 쓴다.
 *
 * 외곽선 ↔ 채움(좋아요, 탭), 더하기 ↔ 빼기(팔로우), 펼침 ↔ 접힘처럼 **모양이 이어지는 쌍**에
 * 쓴다. 원리는 [morphPlan] 에 있다. 서로 관계없는 모양(검색 ↔ 닫기)은 이어 봐야 덩어리가
 * 뭉개질 뿐이라 [RotateSwapIcon] 을 쓴다.
 *
 * 멈춰 있을 때는 원본 벡터를 그대로 그리고, 모핑한 모양은 전환 중에만 그린다. 색도 함께
 * 옮긴다 — 좋아요처럼 모양과 색이 같이 바뀌는 자리에서 색만 툭 바뀌지 않게.
 *
 * [PlayPauseMorphIcon] 과 마찬가지로 **같은 컴포지션 자리에 머물러야** 움직인다. 상태마다
 * 다른 갈래에서 부르지 말고, 한 자리에서 [imageVector] 만 바꿔 넘긴다.
 */
@Composable
fun MorphIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    val animatedTint by animateColorAsState(tint, tween(MorphDurationMs), label = "MorphIconTint")
    val state = remember { MorphIconState(imageVector) }
    LaunchedEffect(imageVector) { state.morphTo(imageVector) }

    val painter = rememberVectorPainter(state.target)
    val path = remember { Path() }

    Canvas(modifier = modifier.iconSemantics(contentDescription).iconSize(imageVector)) {
        val plan = state.plan
        if (plan == null) {
            with(painter) { draw(size, colorFilter = ColorFilter.tint(animatedTint)) }
        } else {
            plan.fill(state.progress.value, size.width, size.height, path)
            drawPath(path = path, color = animatedTint)
        }
    }
}

@Stable
private class MorphIconState(initial: ImageVector) {
    /** 지금 향하고 있는(멈춰 있으면 지금 보이는) 아이콘. */
    var target by mutableStateOf(initial)
        private set

    /** 전환 중일 때만 있다. null 이면 [target] 을 원본 그대로 그린다. */
    var plan by mutableStateOf<MorphPlan?>(null)
        private set

    val progress = Animatable(1f)

    suspend fun morphTo(next: ImageVector) {
        if (next == target) return

        // 전환 도중이면 지금 보이는 모양에서 출발한다. 이전 목표에서 출발하면 모양이 튄다.
        val from = plan?.shapeAt(progress.value) ?: target.toMorphShape()
        target = next
        // 이을 수 없으면(경로 연산이 안 되는 환경) 새 아이콘을 바로 그린다.
        plan = morphPlan(from, next.toMorphShape())
        if (plan == null) {
            progress.snapTo(1f)
            return
        }

        progress.snapTo(0f)
        progress.animateTo(1f, tween(MorphDurationMs, easing = FastOutSlowInEasing))
        // 다 옮겨 갔으면 원본을 그린다. 도중에 취소되면(목표가 또 바뀌면) 여기 오지 않고, 다음
        // morphTo 가 이 plan 의 현재 모양에서 이어 간다.
        plan = null
    }
}

/**
 * 모양이 서로 관계없는 두 아이콘(검색 ↔ 닫기, 다운로드 ↔ 완료)을 돌리며 바꾸는 아이콘.
 *
 * 이전 아이콘이 시계 방향으로 90° 돌며 작아지고, 가장 작아진 순간 새 아이콘으로 바뀌어 -90° 에서
 * 제자리로 돌며 커진다. 돌아가는 방향이 끊기지 않아 한 번의 회전으로 보인다. 겹쳐 흐리게 바꾸지
 * 않는다 — 두 모양이 반투명으로 겹친 순간이 생기지 않는다.
 */
@Composable
fun RotateSwapIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    val animatedTint by animateColorAsState(tint, tween(MorphDurationMs), label = "RotateSwapIconTint")
    val state = remember { RotateSwapState(imageVector) }
    LaunchedEffect(imageVector) { state.swapTo(imageVector) }

    val fromPainter = rememberVectorPainter(state.from)
    val targetPainter = rememberVectorPainter(state.target)

    Canvas(modifier = modifier.iconSemantics(contentDescription).iconSize(imageVector)) {
        val progress = state.progress.value
        val isFirstHalf = progress < 0.5f
        val half = if (isFirstHalf) progress * 2f else (progress - 0.5f) * 2f
        val angle = if (isFirstHalf) 90f * half else -90f * (1f - half)
        val scale = if (isFirstHalf) lerp(1f, RotateSwapMinScale, half) else lerp(RotateSwapMinScale, 1f, half)
        val painter = if (isFirstHalf) fromPainter else targetPainter

        withTransform({
            rotate(angle)
            scale(scaleX = scale, scaleY = scale)
        }) {
            with(painter) { draw(size, colorFilter = ColorFilter.tint(animatedTint)) }
        }
    }
}

/** 바뀌는 순간의 크기. 가장 작을 때 갈아 끼워 교체가 눈에 덜 띈다. */
private const val RotateSwapMinScale = 0.5f

@Stable
private class RotateSwapState(initial: ImageVector) {
    var from by mutableStateOf(initial)
        private set
    var target by mutableStateOf(initial)
        private set

    val progress = Animatable(1f)

    suspend fun swapTo(next: ImageVector) {
        if (next == target) return
        // 전환 도중이면 지금 보이는 쪽에서 다시 돈다.
        from = if (progress.value < 0.5f) from else target
        target = next
        progress.snapTo(0f)
        progress.animateTo(1f, tween(MorphDurationMs, easing = FastOutSlowInEasing))
    }
}

/** `Icon` 과 같은 접근성 표시. 설명이 없으면 장식으로 보고 아무것도 달지 않는다. */
private fun Modifier.iconSemantics(contentDescription: String?): Modifier =
    if (contentDescription == null) {
        this
    } else {
        semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    }

/**
 * `Icon` 과 같은 기본 크기. 호출자가 크기를 정했으면 그쪽이 먼저 걸려 이 값은 무시된다.
 */
private fun Modifier.iconSize(imageVector: ImageVector): Modifier =
    size(width = imageVector.defaultWidth, height = imageVector.defaultHeight)

@ThemePreviews
@Composable
private fun PlayPauseMorphIconPreview() {
    EpisodiveTheme {
        // 가운데(0.5)가 모핑인지 교체인지를 가르는 유일한 프레임이다.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { progress ->
                val path = remember { Path() }
                Canvas(modifier = Modifier.size(48.dp)) {
                    drawPlayPause(progress = progress, color = Color.Gray, path = path)
                }
            }
        }
    }
}
