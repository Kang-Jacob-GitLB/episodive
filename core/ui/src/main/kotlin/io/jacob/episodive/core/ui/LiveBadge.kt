package io.jacob.episodive.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** 라이브 방송 표지. 라디오 미니바와 라디오 플레이어 시트가 함께 쓴다. */
@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 5.dp, vertical = 1.dp),
        text = stringResource(R.string.core_ui_station_live),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        maxLines = 1,
    )
}
