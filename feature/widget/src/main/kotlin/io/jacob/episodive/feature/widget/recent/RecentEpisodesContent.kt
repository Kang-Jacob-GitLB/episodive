package io.jacob.episodive.feature.widget.recent

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.jacob.episodive.core.domain.widget.EpisodeSnapshot
import io.jacob.episodive.feature.widget.R
import io.jacob.episodive.feature.widget.nowplaying.OpenAppCallback
import io.jacob.episodive.feature.widget.theme.WidgetSurfaceContainer
import io.jacob.episodive.feature.widget.theme.WidgetSurfaceContainerLow

/**
 * 최근 에피소드 위젯 콘텐츠.
 *
 * Episodive 디자인 시스템 연결:
 * - 루트: `surfaceContainer` 카드 (16dp 라운드, 12dp padding)
 * - 헤더: `primary` 빨강 14sp Bold
 * - 행: 44dp artwork (8dp 라운드) / title 13sp SemiBold / feed 11sp onSurfaceVariant
 * - 행간 Spacer 6dp
 *
 * 규칙:
 * - snapshot 이 비어있으면 빈 상태 카드.
 * - 채워진 상태는 상단 헤더 + 최대 [MAX_VISIBLE] 개의 행.
 * - 각 행 탭 → MainActivity 오픈 (episode 딥링크는 V2 이월).
 */
@Composable
fun RecentEpisodesContent(
    snapshots: List<EpisodeSnapshot>,
    artworks: Map<Long, Bitmap?>,
) {
    if (snapshots.isEmpty()) {
        EmptyRecentEpisodes()
    } else {
        FilledRecentEpisodes(snapshots, artworks)
    }
}

@Composable
private fun EmptyRecentEpisodes() {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(4.dp),
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetSurfaceContainerLow)
                .cornerRadius(16.dp)
                .padding(14.dp)
                .clickable(actionRunCallback<OpenAppCallback>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = context.getString(R.string.feature_widget_recent_episodes_header),
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = context.getString(R.string.feature_widget_recent_episodes_empty),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = context.getString(R.string.feature_widget_recent_episodes_empty_hint),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FilledRecentEpisodes(
    snapshots: List<EpisodeSnapshot>,
    artworks: Map<Long, Bitmap?>,
) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(4.dp),
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetSurfaceContainer)
                .cornerRadius(16.dp)
                .padding(12.dp),
        ) {
            Text(
                text = context.getString(R.string.feature_widget_recent_episodes_header),
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            snapshots.take(MAX_VISIBLE).forEachIndexed { index, snapshot ->
                if (index > 0) {
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
                EpisodeRow(snapshot, artworks[snapshot.id])
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    snapshot: EpisodeSnapshot,
    artwork: Bitmap?,
) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionRunCallback<OpenAppCallback>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            bitmap = artwork,
            contentDescription = context.getString(
                R.string.feature_widget_recent_episodes_artwork_desc,
            ),
        )
        Spacer(modifier = GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = snapshot.title,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            snapshot.feedTitle?.takeIf { it.isNotBlank() }?.let { feed ->
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = feed,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun Artwork(bitmap: Bitmap?, contentDescription: String) {
    val provider = if (bitmap != null) {
        ImageProvider(bitmap)
    } else {
        ImageProvider(R.drawable.feature_widget_ic_placeholder)
    }
    Image(
        provider = provider,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = GlanceModifier
            .size(44.dp)
            .cornerRadius(8.dp),
    )
}

private const val MAX_VISIBLE = 4
