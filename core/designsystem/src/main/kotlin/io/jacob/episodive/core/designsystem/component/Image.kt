package io.jacob.episodive.core.designsystem.component

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.Px
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.ThemePreviews
import timber.log.Timber

enum class DominantRegion {
    Top,
    Bottom,
    Left,
    Right,
    Center,
    Full
}

private fun extractDominantColor(
    bitmap: Bitmap,
    region: DominantRegion,
    clearFilters: Boolean,
    brightnessAdjustment: Float,
): Color? {
    val paletteBuilder = Palette.from(bitmap).apply {
        if (clearFilters) {
            clearFilters()
        }
    }

    val palette = when (region) {
        DominantRegion.Top -> {
            val regionHeight = (bitmap.height * 0.1f).toInt()
            paletteBuilder.setRegion(
                0,
                0,
                bitmap.width,
                regionHeight
            ).generate()
        }

        DominantRegion.Bottom -> {
            val regionHeight = (bitmap.height * 0.1f).toInt()
            paletteBuilder.setRegion(
                0,
                bitmap.height - regionHeight,
                bitmap.width,
                bitmap.height
            ).generate()
        }

        DominantRegion.Left -> {
            val regionWidth = (bitmap.width * 0.1f).toInt()
            paletteBuilder.setRegion(
                0,
                0,
                regionWidth,
                bitmap.height
            ).generate()
        }

        DominantRegion.Right -> {
            val regionWidth = (bitmap.width * 0.1f).toInt()
            paletteBuilder.setRegion(
                bitmap.width - regionWidth,
                0,
                bitmap.width,
                bitmap.height
            ).generate()
        }

        DominantRegion.Center -> {
            val regionWidth = (bitmap.width * 0.5f).toInt()
            val regionHeight = (bitmap.height * 0.5f).toInt()
            val left = (bitmap.width - regionWidth) / 2
            val top = (bitmap.height - regionHeight) / 2
            paletteBuilder.setRegion(
                left,
                top,
                left + regionWidth,
                top + regionHeight
            ).generate()
        }

        DominantRegion.Full -> {
            paletteBuilder.generate()
        }
    }

    return palette.dominantSwatch?.let { swatch ->
        val baseColor = Color(swatch.rgb)
        adjustBrightness(baseColor, brightnessAdjustment)
    }
}

private fun adjustBrightness(color: Color, adjustment: Float): Color {
    if (adjustment == 0f) return color

    val red = color.red
    val green = color.green
    val blue = color.blue

    return if (adjustment > 0) {
        // 밝게: 흰색으로 interpolation
        Color(
            red = red + (1f - red) * adjustment,
            green = green + (1f - green) * adjustment,
            blue = blue + (1f - blue) * adjustment,
            alpha = color.alpha
        )
    } else {
        // 어둡게: 검은색으로 interpolation
        val factor = 1f + adjustment
        Color(
            red = red * factor,
            green = green * factor,
            blue = blue * factor,
            alpha = color.alpha
        )
    }
}

@Composable
fun StateImage(
    modifier: Modifier = Modifier,
    imageUrl: String,
    @Px size: Int = 300,
    contentDescription: String?,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderBrush: Brush = thumbnailPlaceholderDefaultBrush(),
    fallbackIcon: ImageVector = EpisodiveIcons.Error,
    onDominantColorExtracted: ((Color) -> Unit)? = null,
    dominantRegion: DominantRegion = DominantRegion.Bottom,
    clearFilters: Boolean = true,
    brightnessAdjustment: Float = 0f,
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier.background(placeholderBrush))
        return
    }

    var imagePainterState by remember {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }

    val context = LocalContext.current
    val imageLoader = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .size(size)
            .apply {
                if (onDominantColorExtracted != null) {
                    allowHardware(false)
                    listener(
                        onSuccess = { _, result ->
                            val drawable = result.drawable
                            val bitmap = (drawable as? BitmapDrawable)?.bitmap
                            if (bitmap != null) {
                                val color = extractDominantColor(
                                    bitmap,
                                    dominantRegion,
                                    clearFilters,
                                    brightnessAdjustment
                                )
                                color?.let(onDominantColorExtracted)
                            }
                        }
                    )
                }
            }
            .build(),
        contentScale = contentScale,
        onState = { state -> imagePainterState = state }
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (imagePainterState) {
            is AsyncImagePainter.State.Empty,
            is AsyncImagePainter.State.Loading,
                -> {
            }

            is AsyncImagePainter.State.Error,
                -> {
                Timber.w("Image($imageUrl) load error: ${(imagePainterState as? AsyncImagePainter.State.Error)?.result?.throwable.toString()}")
                Box(
                    modifier = Modifier
                        .background(placeholderBrush)
                        .fillMaxSize()
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(8.dp),
                        imageVector = fallbackIcon,
                        contentDescription = null,
                    )
                }
            }

            is AsyncImagePainter.State.Success -> {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.onBackground)
                        .fillMaxSize()
                )
            }
        }

        Image(
            painter = imageLoader,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier
                .fillMaxSize(),
        )
    }
}

@Composable
internal fun thumbnailPlaceholderDefaultBrush(
    color: Color = MaterialTheme.colorScheme.secondaryContainer
): Brush {
    return SolidColor(color)
}

@ThemePreviews
@Composable
private fun StateImagePreview() {
    EpisodiveTheme {
        StateImage(
            imageUrl = "https://www.example.com/image.jpg",
            contentDescription = "Example Image",
            modifier = Modifier
                .size(16.dp)
        )
    }
}