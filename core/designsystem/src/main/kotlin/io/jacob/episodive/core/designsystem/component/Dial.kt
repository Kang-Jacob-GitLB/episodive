package io.jacob.episodive.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Dial 상태를 관리하는 클래스
 */
@Stable
private class DialState(
    val offset: Animatable<Float, *>,
    val snapValues: List<Float>,
    val steps: Int,
    var isDragging: Boolean = false,
) {
    fun getValueIndex(value: Float): Int {
        return snapValues.indexOfFirst { abs(it - value) < 0.01f }.takeIf { it >= 0 } ?: 0
    }

    fun getValueAtIndex(index: Int): Float {
        return snapValues.getOrNull(index) ?: snapValues.first()
    }
}

@Composable
private fun rememberDialState(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
): DialState {
    val snapValues = remember(range, steps) {
        (0..steps).map { range.start + (range.endInclusive - range.start) * it / steps }
    }

    val state = remember(steps) {
        DialState(
            offset = Animatable(0f),
            snapValues = snapValues,
            steps = steps
        )
    }

    // value가 변경되면 offset도 업데이트
    LaunchedEffect(value, state.isDragging) {
        if (!state.isDragging) {
            val targetIndex = state.getValueIndex(value)
            state.offset.animateTo(
                targetValue = targetIndex.toFloat(),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    return state
}

@Composable
fun EpisodiveDial(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0.5f..3.5f,
    steps: Int = 30,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    unselectedColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val state = rememberDialState(value, range, steps)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
    ) {
        val width = constraints.maxWidth.toFloat()
        val stepWidth = with(density) { 30.dp.toPx() }
        val centerX = width / 2f

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .dialGesture(
                    state = state,
                    scope = scope,
                    stepWidth = stepWidth,
                    onValueChange = onValueChange
                )
        ) {
            drawDialContent(
                state = state,
                centerX = centerX,
                width = width,
                stepWidth = stepWidth,
                selectedColor = selectedColor,
                unselectedColor = unselectedColor,
            )
        }
    }
}

/**
 * Dial 제스처 처리를 위한 Modifier
 */
private fun Modifier.dialGesture(
    state: DialState,
    scope: CoroutineScope,
    stepWidth: Float,
    onValueChange: (Float) -> Unit,
): Modifier = this.pointerInput(Unit, stepWidth) {
    val velocityTracker = VelocityTracker()

    awaitEachGesture {
        val down = awaitFirstDown()
        state.isDragging = true
        velocityTracker.resetTracking()

        drag(down.id) { change ->
            velocityTracker.addPosition(change.uptimeMillis, change.position)
            val dragDelta = change.position.x - change.previousPosition.x

            scope.launch {
                val newOffset = (state.offset.value - dragDelta / stepWidth)
                    .coerceIn(0f, state.steps.toFloat())
                state.offset.snapTo(newOffset)

                // 드래그 중에도 실시간으로 값 전달
                val currentIndex = newOffset.roundToInt().coerceIn(0, state.steps)
                onValueChange(state.getValueAtIndex(currentIndex))
            }
            change.consume()
        }

        state.isDragging = false
        val velocity = velocityTracker.calculateVelocity()

        scope.launch {
            // Fling 애니메이션
            val decay = exponentialDecay<Float>(frictionMultiplier = 3f)
            state.offset.animateDecay(
                initialVelocity = -velocity.x / stepWidth,
                animationSpec = decay
            )

            // 가장 가까운 눈금으로 스냅
            val targetIndex = state.offset.value.roundToInt().coerceIn(0, state.steps)
            state.offset.animateTo(
                targetValue = targetIndex.toFloat(),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            onValueChange(state.getValueAtIndex(targetIndex))
        }
    }
}

/**
 * Dial의 모든 그리기 내용
 */
private fun DrawScope.drawDialContent(
    state: DialState,
    centerX: Float,
    width: Float,
    stepWidth: Float,
    selectedColor: Color,
    unselectedColor: Color,
) {
    val fadeWidth = 160.dp.toPx()
    val textPaint = createTextPaint()

    // 눈금과 숫자 그리기
    for (i in 0..state.steps) {
        val x = centerX + (i - state.offset.value) * stepWidth

        // 화면 밖의 눈금은 그리지 않음
        if (x < -stepWidth || x > width + stepWidth) continue

        val distanceFromCenter = abs(x - centerX)
        val isSelected = distanceFromCenter < stepWidth / 2f
        val fadeRatio = calculateFadeRatio(x, width, fadeWidth)

        drawTick(x, isSelected, fadeRatio, selectedColor, unselectedColor)

        // 숫자 표시 (매 5번째마다)
        if (i % 5 == 0) {
            drawNumber(x, state.snapValues[i], fadeRatio, textPaint)
        }
    }

    // 중앙 포커스 인디케이터
    drawCenterIndicator(centerX, selectedColor)
}

/**
 * 텍스트 페인트 생성
 */
private fun DrawScope.createTextPaint(): android.graphics.Paint {
    return Paint().asFrameworkPaint().apply {
        color = android.graphics.Color.WHITE
        textSize = 12.sp.toPx()
        textAlign = android.graphics.Paint.Align.CENTER
    }
}

/**
 * Fade ratio 계산
 */
private fun calculateFadeRatio(x: Float, width: Float, fadeWidth: Float): Float {
    return when {
        x < fadeWidth -> (x / fadeWidth).coerceIn(0f, 1f)
        x > width - fadeWidth -> ((width - x) / fadeWidth).coerceIn(0f, 1f)
        else -> 1f
    }
}

/**
 * 눈금 그리기
 */
private fun DrawScope.drawTick(
    x: Float,
    isSelected: Boolean,
    fadeRatio: Float,
    selectedColor: Color,
    unselectedColor: Color,
) {
    drawLine(
        color = if (isSelected) selectedColor else unselectedColor.copy(alpha = fadeRatio),
        start = Offset(x, 70f),
        end = Offset(x, if (isSelected) 55.dp.toPx() else 55.dp.toPx()),
        strokeWidth = if (isSelected) 3.dp.toPx() else 2.dp.toPx(),
        cap = StrokeCap.Round
    )
}

/**
 * 숫자 그리기
 */
private fun DrawScope.drawNumber(
    x: Float,
    number: Float,
    fadeRatio: Float,
    textPaint: android.graphics.Paint,
) {
    val numberText = "%.1f".format(number)

    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(
            numberText,
            x,
            75.dp.toPx(),
            textPaint.apply { alpha = (255 * fadeRatio).toInt() }
        )
    }
}

/**
 * 중앙 포커스 인디케이터 그리기 (역 정삼각형)
 */
private fun DrawScope.drawCenterIndicator(
    centerX: Float,
    color: Color,
) {
    val triangleHeight = 8.dp.toPx()
    val triangleWidth = 14.dp.toPx()
    val topY = 5.dp.toPx()

    val path = Path().apply {
        // 아래쪽 꼭지점 (180도 회전)
        moveTo(centerX, topY + triangleHeight)
        // 왼쪽 위 꼭지점
        lineTo(centerX - triangleWidth / 2f, topY)
        // 오른쪽 위 꼭지점
        lineTo(centerX + triangleWidth / 2f, topY)
        // 다시 아래쪽 꼭지점으로 닫기
        close()
    }

    drawPath(
        path = path,
        color = color
    )
}

@DevicePreviews
@Composable
private fun EpisodiveDialPreview() {
    EpisodiveTheme {
        EpisodiveDial(
            value = 1f,
            onValueChange = {}
        )
    }
}