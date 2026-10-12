package io.jacob.episodive.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.jacob.episodive.core.designsystem.theme.EpisodiveShapes
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.theme.surfaceContainerHighDark

/**
 * 미니플레이어 틴트의 바탕이 되는 표면색. 전경(제목·부제·좋아요·진행바 트랙)이 전부
 * 흰색이라 라이트/다크 어느 테마에서도 어두워야 한다 — 다크 스킴 값으로 고정한다.
 */
private val MiniBarTintSurface = surfaceContainerHighDark

/** 강조색을 미니플레이어 표면색 쪽으로 섞는 비율 (0 = 강조색 그대로). */
private const val MiniBarTintStartBlend = 0.45f
private const val MiniBarTintEndBlend = 0.9f

/**
 * 화면 아래에 떠 있는 미니바(에피소드·라디오)의 겉모양. 안쪽 구성은 [content] 가 정한다.
 *
 * 미니바는 콘텐츠 위에 떠 있는 어두운 틴트 칩이고 전경이 전부 흰색이다. 블렌드 대상을
 * 테마 표면색으로 두면 라이트 테마에서 배경이 근백색이 되어 글자가 사라지므로, 테마와 무관한
 * 다크 표면색으로 고정한다. [accent] 를 그대로 깔지 않고 표면색 쪽으로 눌러 쓰는 이유는,
 * 밝은 색 위에서는 흰 글자가 묻히기 때문이다.
 */
@Composable
fun MiniBarContainer(
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val dimension = LocalDimensionTheme.current
    val surfaceColor = MiniBarTintSurface
    val barGradient = remember(accent, surfaceColor) {
        Brush.linearGradient(
            colors = listOf(
                lerp(accent, surfaceColor, MiniBarTintStartBlend),
                lerp(accent, surfaceColor, MiniBarTintEndBlend),
            ),
            start = Offset.Zero,
            end = Offset(1000f, 176f),
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(dimension.playerBarHeight)
            .padding(horizontal = dimension.playerBarMargin),
        shape = EpisodiveShapes.miniPlayer,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(barGradient),
            content = content,
        )
    }
}

/**
 * 미니바의 재생/일시정지 버튼.
 *
 * 준비 중에는 [isPlaying] 이 거짓이다. 재생 아이콘을 그대로 두면 누른 재생이 먹히지 않은 것처럼
 * 보여 스피너를 띄운다.
 */
@Composable
fun MiniBarPlayPauseButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EpisodiveIconToggleButton(
        modifier = modifier.size(38.dp),
        checked = isPlaying,
        onCheckedChange = { onClick() },
        colors = IconButtonDefaults.iconToggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.primary,
            checkedContentColor = Color.White,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
        ),
        icon = {
            val isLoading = !isPlaying && isBuffering
            Box(contentAlignment = Alignment.Center) {
                // 스피너가 아이콘 자리를 빼앗지 않고 위에 겹친다. 갈래를 나누면 준비를
                // 거쳐 재생이 시작될 때 아이콘이 새로 붙어 재생→일시정지가 움직이지 못한다.
                Box(modifier = if (isLoading) Modifier.hiddenWhileLoading() else Modifier) {
                    PlayPauseMorphIcon(
                        modifier = Modifier.size(18.dp),
                        isPlaying = isPlaying,
                        tint = Color.White,
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .semantics { contentDescription = "Loading" },
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                }
            }
        },
    )
}

/**
 * 재생 준비 중 스피너 밑에 깔린 재생 아이콘을 감춘다. 보이지만 않게 하는 것이 아니라 접근성
 * 트리에서도 뺀다 — 남겨 두면 스크린리더가 "Loading" 과 "Play" 를 함께 읽는다.
 */
fun Modifier.hiddenWhileLoading(): Modifier = alpha(0f).clearAndSetSemantics {}
