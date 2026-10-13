package io.jacob.episodive.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.jacob.episodive.core.designsystem.theme.EpisodiveShapes

/** v2: 배속·슬립타이머 시트가 공유하는 원형 프리셋 (원본 줄 396, 415). */
@Composable
fun PresetCircle(
    modifier: Modifier = Modifier,
    size: Dp,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(CircleShape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    // 시트 배경(surfaceContainer)과 한 단계 차이인 surfaceContainerHigh 로는
                    // 버튼 경계가 보이지 않는다. 두 단계 위 표면에 테두리까지 둘러 분리한다.
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
            )
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        CircleShape,
                    )
                },
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/** v2: 슬립타이머 시트의 "에피소드 끝까지" / "타이머 취소" pill 버튼(원본 줄 422~423). */
@Composable
fun SheetActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector,
    text: String,
    fontWeight: FontWeight = FontWeight.SemiBold,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(EpisodiveShapes.pill)
            .background(containerColor)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = fontWeight),
            color = contentColor,
        )
    }
}
