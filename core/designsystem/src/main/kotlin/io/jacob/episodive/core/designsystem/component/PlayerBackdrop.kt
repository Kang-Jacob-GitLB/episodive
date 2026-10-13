package io.jacob.episodive.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val PlayerBackdropAngle = 193f

/** 그라디언트 중간·어두운 스톱이 커버 색에서 배경으로 섞이는 비율. */
private const val PlayerBackdropMidBlend = 0.42f
private const val PlayerBackdropDarkBlend = 0.78f

/**
 * 플레이어 화면 커버 영역의 배경. 193도 · 4단계 스톱 구조를 유지하되, 중간 스톱까지 전부
 * 앨범아트 팔레트 색([top])에서 파생시킨다. 고정 레드 스톱을 끼우면 커버가 무슨 색이든
 * 배경이 늘 붉게 나온다. 에피소드·라디오 플레이어가 같은 모양을 쓴다.
 */
fun Modifier.playerBackdrop(top: Color, background: Color): Modifier = drawWithCache {
    val (start, end) = angledGradientEndpoints(PlayerBackdropAngle, size)
    val brush = Brush.linearGradient(
        0f to top,
        0.30f to lerp(top, background, PlayerBackdropMidBlend),
        0.58f to lerp(top, background, PlayerBackdropDarkBlend),
        0.82f to background,
        start = start,
        end = end,
    )
    onDrawBehind { drawRect(brush) }
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
