package io.jacob.episodive.core.designsystem.component

import android.annotation.SuppressLint
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@SuppressLint("UnnecessaryComposedModifier")
fun Modifier.fadingEdgeMarquee(
    fadeWidth: Dp = 12.dp,
) = composed {
    val density = LocalDensity.current

    this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val fadeWidthPx = with(density) { fadeWidth.toPx() }
            val fadeRatio = fadeWidthPx / size.width

            drawRect(
                brush = Brush.horizontalGradient(
                    0.0f to Color.Transparent,
                    fadeRatio to Color.Black,
                    1f - fadeRatio to Color.Black,
                    1.0f to Color.Transparent
                ),
                blendMode = BlendMode.DstIn
            )
        }
        .basicMarquee(
            iterations = Int.MAX_VALUE,
            initialDelayMillis = 2000,
            repeatDelayMillis = 2000,
        )
}