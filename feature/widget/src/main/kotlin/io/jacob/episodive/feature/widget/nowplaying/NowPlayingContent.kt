package io.jacob.episodive.feature.widget.nowplaying

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.background
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.GlanceTheme
import androidx.glance.semantics.semantics
import androidx.glance.semantics.contentDescription
import androidx.glance.LocalContext
import androidx.compose.runtime.remember
import androidx.glance.appwidget.action.ActionCallback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.IconCompat
import io.jacob.episodive.core.domain.widget.NowPlayingSnapshot
import io.jacob.episodive.feature.widget.PlaybackControl
import io.jacob.episodive.feature.widget.R
import io.jacob.episodive.feature.widget.action.WidgetActionCallback

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
            .background(GlanceTheme.colors.surface)
            .padding(12.dp)
            .clickable(actionRunCallback<OpenAppCallback>()),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = context.getString(R.string.feature_widget_now_playing_empty),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
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
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            bitmap = artwork,
            contentDescription = context.getString(
                R.string.feature_widget_now_playing_artwork_desc,
            ),
        )
        Spacer(modifier = GlanceModifier.width(10.dp))
        Column(
            modifier = GlanceModifier.defaultWeight(),
        ) {
            Text(
                text = snapshot.title,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
            snapshot.feedTitle?.takeIf { it.isNotBlank() }?.let { feed ->
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = feed,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                    ),
                    maxLines = 1,
                )
            }
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        ControlButton(
            iconRes = R.drawable.feature_widget_ic_rewind,
            contentDescription = context.getString(
                R.string.feature_widget_now_playing_seek_backward_desc,
            ),
            control = PlaybackControl.SEEK_BWD,
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        ControlButton(
            iconRes = if (snapshot.isPlaying) {
                R.drawable.feature_widget_ic_pause
            } else {
                R.drawable.feature_widget_ic_play
            },
            contentDescription = context.getString(
                if (snapshot.isPlaying) {
                    R.string.feature_widget_now_playing_pause_desc
                } else {
                    R.string.feature_widget_now_playing_play_desc
                },
            ),
            control = PlaybackControl.PLAY_PAUSE,
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        ControlButton(
            iconRes = R.drawable.feature_widget_ic_fast_forward,
            contentDescription = context.getString(
                R.string.feature_widget_now_playing_seek_forward_desc,
            ),
            control = PlaybackControl.SEEK_FWD,
        )
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
            .size(48.dp)
            .cornerRadius(8.dp)
            .clickable(actionRunCallback<OpenAppCallback>()),
    )
}

@Composable
private fun ControlButton(
    iconRes: Int,
    contentDescription: String,
    control: PlaybackControl,
) {
    Image(
        provider = ImageProvider(iconRes),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
        modifier = GlanceModifier
            .size(40.dp)
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
