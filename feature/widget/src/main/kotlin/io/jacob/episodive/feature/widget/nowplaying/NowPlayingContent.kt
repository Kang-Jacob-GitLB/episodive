package io.jacob.episodive.feature.widget.nowplaying

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
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
import io.jacob.episodive.core.domain.widget.NowPlayingSnapshot
import io.jacob.episodive.feature.widget.PlaybackControl
import io.jacob.episodive.feature.widget.R
import io.jacob.episodive.feature.widget.action.WidgetActionCallback
import io.jacob.episodive.feature.widget.theme.WidgetSurfaceContainer
import io.jacob.episodive.feature.widget.theme.WidgetSurfaceContainerLow

/**
 * 현재 재생 중 위젯 콘텐츠.
 *
 * Episodive 디자인 시스템 연결:
 * - 배경: `surfaceContainer` 카드 (16dp 라운드)
 * - Play: `primary` 빨강 accent (#F5332C)
 * - Seek: `onSurface` 중성 톤
 * - 타이포: 15sp SemiBold 제목 / 12sp onSurfaceVariant 피드명
 */
@Composable
fun NowPlayingContent(
    snapshot: NowPlayingSnapshot?,
    artwork: Bitmap?,
) {
    if (snapshot == null) {
        EmptyNowPlaying()
    } else {
        FilledNowPlaying(snapshot, artwork)
    }
}

@Composable
private fun EmptyNowPlaying() {
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                provider = ImageProvider(R.drawable.feature_widget_ic_placeholder),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier
                    .size(56.dp)
                    .cornerRadius(10.dp),
            )
            Spacer(modifier = GlanceModifier.height(10.dp))
            Text(
                text = context.getString(R.string.feature_widget_now_playing_empty),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = context.getString(R.string.feature_widget_now_playing_empty_hint),
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
private fun FilledNowPlaying(
    snapshot: NowPlayingSnapshot,
    artwork: Bitmap?,
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
                .padding(14.dp),
        ) {
            // Row 1: artwork + 제목/피드 (상단 영역, weight=1)
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(
                    bitmap = artwork,
                    contentDescription = context.getString(
                        R.string.feature_widget_now_playing_artwork_desc,
                    ),
                )
                Spacer(modifier = GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = snapshot.title,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 2,
                    )
                    snapshot.feedTitle?.takeIf { it.isNotBlank() }?.let { feed ->
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = feed,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }
            // Row 2: 재생 컨트롤 (중앙, 하단)
            Spacer(modifier = GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SeekButton(
                    iconRes = R.drawable.feature_widget_ic_rewind,
                    contentDescription = context.getString(
                        R.string.feature_widget_now_playing_seek_backward_desc,
                    ),
                    control = PlaybackControl.SEEK_BWD,
                )
                Spacer(modifier = GlanceModifier.width(10.dp))
                PlayPauseButton(
                    isPlaying = snapshot.isPlaying,
                    contentDescription = context.getString(
                        if (snapshot.isPlaying) {
                            R.string.feature_widget_now_playing_pause_desc
                        } else {
                            R.string.feature_widget_now_playing_play_desc
                        },
                    ),
                )
                Spacer(modifier = GlanceModifier.width(10.dp))
                SeekButton(
                    iconRes = R.drawable.feature_widget_ic_fast_forward,
                    contentDescription = context.getString(
                        R.string.feature_widget_now_playing_seek_forward_desc,
                    ),
                    control = PlaybackControl.SEEK_FWD,
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
            .size(52.dp)
            .cornerRadius(10.dp)
            .clickable(actionRunCallback<OpenAppCallback>()),
    )
}

/**
 * Primary 빨강 accent 를 가진 재생/일시정지 버튼.
 * 아이콘 크기를 컨테이너 중앙에 얹기 위해 Box 로 감싼다.
 */
@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    contentDescription: String,
) {
    val iconRes = if (isPlaying) {
        R.drawable.feature_widget_ic_pause
    } else {
        R.drawable.feature_widget_ic_play
    }
    Box(
        modifier = GlanceModifier
            .size(44.dp)
            .cornerRadius(22.dp)
            .background(GlanceTheme.colors.primary)
            .clickable(
                actionRunCallback<WidgetActionCallback>(
                    parameters = actionParametersOf(
                        PlaybackControl.KEY to PlaybackControl.PLAY_PAUSE.name,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
            modifier = GlanceModifier.size(22.dp),
        )
    }
}

/**
 * 중성 톤의 ±15s / ±30s seek 버튼.
 */
@Composable
private fun SeekButton(
    iconRes: Int,
    contentDescription: String,
    control: PlaybackControl,
) {
    Image(
        provider = ImageProvider(iconRes),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface),
        modifier = GlanceModifier
            .size(40.dp)
            .padding(8.dp)
            .clickable(
                actionRunCallback<WidgetActionCallback>(
                    parameters = actionParametersOf(
                        PlaybackControl.KEY to control.name,
                    ),
                ),
            ),
    )
}

/**
 * 빈 상태 / 아트워크 탭 시 MainActivity 를 연다.
 */
class OpenAppCallback : ActionCallback {
    override suspend fun onAction(
        context: android.content.Context,
        glanceId: androidx.glance.GlanceId,
        parameters: ActionParameters,
    ) {
        io.jacob.episodive.feature.widget.action
            .openAppPendingIntent(context, REQ_OPEN_APP)
            .send()
    }

    companion object {
        private const val REQ_OPEN_APP = 99
    }
}
