package io.jacob.episodive.feature.widget.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProvider as DayNightColorProvider
import androidx.glance.material3.ColorProviders
import androidx.glance.unit.ColorProvider
import io.jacob.episodive.core.designsystem.theme.backgroundDark
import io.jacob.episodive.core.designsystem.theme.backgroundLight
import io.jacob.episodive.core.designsystem.theme.errorContainerDark
import io.jacob.episodive.core.designsystem.theme.errorContainerLight
import io.jacob.episodive.core.designsystem.theme.errorDark
import io.jacob.episodive.core.designsystem.theme.errorLight
import io.jacob.episodive.core.designsystem.theme.inverseOnSurfaceDark
import io.jacob.episodive.core.designsystem.theme.inverseOnSurfaceLight
import io.jacob.episodive.core.designsystem.theme.inversePrimaryDark
import io.jacob.episodive.core.designsystem.theme.inversePrimaryLight
import io.jacob.episodive.core.designsystem.theme.inverseSurfaceDark
import io.jacob.episodive.core.designsystem.theme.inverseSurfaceLight
import io.jacob.episodive.core.designsystem.theme.onBackgroundDark
import io.jacob.episodive.core.designsystem.theme.onBackgroundLight
import io.jacob.episodive.core.designsystem.theme.onErrorContainerDark
import io.jacob.episodive.core.designsystem.theme.onErrorContainerLight
import io.jacob.episodive.core.designsystem.theme.onErrorDark
import io.jacob.episodive.core.designsystem.theme.onErrorLight
import io.jacob.episodive.core.designsystem.theme.onPrimaryContainerDark
import io.jacob.episodive.core.designsystem.theme.onPrimaryContainerLight
import io.jacob.episodive.core.designsystem.theme.onPrimaryDark
import io.jacob.episodive.core.designsystem.theme.onPrimaryLight
import io.jacob.episodive.core.designsystem.theme.onSecondaryContainerDark
import io.jacob.episodive.core.designsystem.theme.onSecondaryContainerLight
import io.jacob.episodive.core.designsystem.theme.onSecondaryDark
import io.jacob.episodive.core.designsystem.theme.onSecondaryLight
import io.jacob.episodive.core.designsystem.theme.onSurfaceDark
import io.jacob.episodive.core.designsystem.theme.onSurfaceLight
import io.jacob.episodive.core.designsystem.theme.onSurfaceVariantDark
import io.jacob.episodive.core.designsystem.theme.onSurfaceVariantLight
import io.jacob.episodive.core.designsystem.theme.onTertiaryContainerDark
import io.jacob.episodive.core.designsystem.theme.onTertiaryContainerLight
import io.jacob.episodive.core.designsystem.theme.onTertiaryDark
import io.jacob.episodive.core.designsystem.theme.onTertiaryLight
import io.jacob.episodive.core.designsystem.theme.outlineDark
import io.jacob.episodive.core.designsystem.theme.outlineLight
import io.jacob.episodive.core.designsystem.theme.outlineVariantDark
import io.jacob.episodive.core.designsystem.theme.outlineVariantLight
import io.jacob.episodive.core.designsystem.theme.primaryContainerDark
import io.jacob.episodive.core.designsystem.theme.primaryContainerLight
import io.jacob.episodive.core.designsystem.theme.primaryDark
import io.jacob.episodive.core.designsystem.theme.primaryLight
import io.jacob.episodive.core.designsystem.theme.scrimDark
import io.jacob.episodive.core.designsystem.theme.scrimLight
import io.jacob.episodive.core.designsystem.theme.secondaryContainerDark
import io.jacob.episodive.core.designsystem.theme.secondaryContainerLight
import io.jacob.episodive.core.designsystem.theme.secondaryDark
import io.jacob.episodive.core.designsystem.theme.secondaryLight
import io.jacob.episodive.core.designsystem.theme.surfaceBrightDark
import io.jacob.episodive.core.designsystem.theme.surfaceBrightLight
import io.jacob.episodive.core.designsystem.theme.surfaceContainerDark
import io.jacob.episodive.core.designsystem.theme.surfaceContainerHighDark
import io.jacob.episodive.core.designsystem.theme.surfaceContainerHighLight
import io.jacob.episodive.core.designsystem.theme.surfaceContainerHighestDark
import io.jacob.episodive.core.designsystem.theme.surfaceContainerHighestLight
import io.jacob.episodive.core.designsystem.theme.surfaceContainerLight
import io.jacob.episodive.core.designsystem.theme.surfaceContainerLowDark
import io.jacob.episodive.core.designsystem.theme.surfaceContainerLowLight
import io.jacob.episodive.core.designsystem.theme.surfaceContainerLowestDark
import io.jacob.episodive.core.designsystem.theme.surfaceContainerLowestLight
import io.jacob.episodive.core.designsystem.theme.surfaceDark
import io.jacob.episodive.core.designsystem.theme.surfaceDimDark
import io.jacob.episodive.core.designsystem.theme.surfaceDimLight
import io.jacob.episodive.core.designsystem.theme.surfaceLight
import io.jacob.episodive.core.designsystem.theme.surfaceVariantDark
import io.jacob.episodive.core.designsystem.theme.surfaceVariantLight
import io.jacob.episodive.core.designsystem.theme.tertiaryContainerDark
import io.jacob.episodive.core.designsystem.theme.tertiaryContainerLight
import io.jacob.episodive.core.designsystem.theme.tertiaryDark
import io.jacob.episodive.core.designsystem.theme.tertiaryLight

/**
 * Glance 위젯 전용 Episodive 테마.
 *
 * core/designsystem 의 Material3 ColorScheme 를 Glance ColorProviders 로 bridge 해서,
 * 위젯에서 `GlanceTheme.colors.primary` 가 Episodive 빨강(#F5332C) 으로 노출되도록 한다.
 *
 * 시스템 다크 모드에 따라 light/dark 자동 전환.
 */
private val episodiveLightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val episodiveDarkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

private val episodiveColorProviders = ColorProviders(
    light = episodiveLightScheme,
    dark = episodiveDarkScheme,
)

/**
 * Glance 1.2 의 `GlanceTheme.colors` 는 MD3 expanded tokens
 * (`surfaceContainer*`, `surfaceDim/Bright`, `surfaceContainerLowest`) 를 노출하지 않는다.
 * 위젯 카드 배경에 필요한 토큰은 `androidx.glance.color.ColorProvider(day, night)` 로 직접 bridge.
 */
val WidgetSurfaceContainer: ColorProvider = DayNightColorProvider(
    day = surfaceContainerLight,
    night = surfaceContainerDark,
)

val WidgetSurfaceContainerLow: ColorProvider = DayNightColorProvider(
    day = surfaceContainerLowLight,
    night = surfaceContainerLowDark,
)

@Composable
fun EpisodiveGlanceTheme(content: @Composable () -> Unit) {
    GlanceTheme(colors = episodiveColorProviders, content = content)
}
