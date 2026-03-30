package io.jacob.episodive.core.designsystem.icon.tabler

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Tabler.DeviceFloppyFilled: ImageVector
    get() {
        if (_DeviceFloppyFilled != null) {
            return _DeviceFloppyFilled!!
        }
        _DeviceFloppyFilled = ImageVector.Builder(
            name = "DeviceFloppyFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16.765f, 2f)
                arcToRelative(
                    2f,
                    2f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    1.235f,
                    0.428f
                )
                lineToRelative(0.123f, 0.1f)
                lineToRelative(2.745f, 2.455f)
                arcToRelative(
                    2f,
                    2f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    0.591f,
                    1.132f
                )
                lineToRelative(0.041f, 0.22f)
                lineToRelative(0.007f, 0.165f)
                verticalLineToRelative(11.5f)
                arcToRelative(
                    2f,
                    2f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    -1.85f,
                    1.995f
                )
                lineToRelative(-0.15f, 0.005f)
                horizontalLineToRelative(-12f)
                arcToRelative(
                    2f,
                    2f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    -1.995f,
                    -1.85f
                )
                lineToRelative(-0.005f, -0.15f)
                verticalLineToRelative(-16f)
                arcToRelative(
                    2f,
                    2f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    1.85f,
                    -1.995f
                )
                lineToRelative(0.15f, -0.005f)
                close()
                moveTo(12f, 12f)
                arcToRelative(
                    2f,
                    2f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    -1.977f,
                    1.697f
                )
                lineToRelative(-0.023f, 0.154f)
                lineToRelative(-0.005f, 0.149f)
                lineToRelative(0.005f, 0.15f)
                arcToRelative(
                    2f,
                    2f,
                    0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    2f,
                    -2.15f
                )
                close()
                moveTo(14f, 4f)
                horizontalLineToRelative(-4f)
                arcToRelative(
                    1f,
                    1f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    -0.993f,
                    0.883f
                )
                lineToRelative(-0.007f, 0.117f)
                verticalLineToRelative(2f)
                arcToRelative(
                    1f,
                    1f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    0.883f,
                    0.993f
                )
                lineToRelative(0.117f, 0.007f)
                horizontalLineToRelative(4f)
                arcToRelative(
                    1f,
                    1f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    0.993f,
                    -0.883f
                )
                lineToRelative(0.007f, -0.117f)
                verticalLineToRelative(-2f)
                arcToRelative(
                    1f,
                    1f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    -0.883f,
                    -0.993f
                )
                lineToRelative(-0.117f, -0.007f)
                close()
            }
        }.build()

        return _DeviceFloppyFilled!!
    }

@Suppress("ObjectPropertyName")
private var _DeviceFloppyFilled: ImageVector? = null
