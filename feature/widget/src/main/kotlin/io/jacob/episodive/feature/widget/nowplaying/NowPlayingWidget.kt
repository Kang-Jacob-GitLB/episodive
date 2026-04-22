package io.jacob.episodive.feature.widget.nowplaying

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.tracing.trace
import dagger.hilt.android.EntryPointAccessors
import io.jacob.episodive.core.domain.widget.WidgetDataReaderEntryPoint
import io.jacob.episodive.feature.widget.image.WidgetImageLoader
import io.jacob.episodive.feature.widget.theme.EpisodiveGlanceTheme

/**
 * 현재 재생 중 위젯.
 *
 * 규약:
 * - `provideGlance` 내부에서는 `Flow.first()` 스냅샷만 사용. 장기 `collect` 금지.
 * - 재렌더 트리거는 외부의 `WidgetUpdater` → `WidgetDispatcher` → `updateAll()` 경로로만 발생.
 * - tracing: `widget.render` 구간 + `WidgetPerf` Logcat 태그.
 */
class NowPlayingWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        trace(TRACE_RENDER) {
            val startMs = SystemClock.uptimeMillis()
            val reader = EntryPointAccessors
                .fromApplication(
                    context.applicationContext,
                    WidgetDataReaderEntryPoint::class.java,
                )
                .widgetDataReader()
            val snapshot = runCatching { reader.snapshotNowPlaying() }
                .onFailure { Log.e(TAG, "snapshotNowPlaying failed", it) }
                .getOrNull()
            val bitmap = snapshot?.imageUrl?.let { url ->
                WidgetImageLoader.loadWidgetBitmap(context, url)
            }

            val deltaMs = SystemClock.uptimeMillis() - startMs
            // provideContent 가 session 종료까지 suspend 하므로 반드시 그 *이전*에 로깅.
            Log.d(
                PERF_TAG,
                "pre-render snapshot=${snapshot != null} " +
                    "episode=${snapshot?.episodeId} " +
                    "imageUrl=${snapshot?.imageUrl} " +
                    "bitmap=${bitmap != null} " +
                    "deltaMs=$deltaMs",
            )

            provideContent {
                EpisodiveGlanceTheme {
                    NowPlayingContent(snapshot, bitmap)
                }
            }
        }
    }

    private companion object {
        const val TAG = "NowPlayingWidget"
        const val PERF_TAG = "WidgetPerf"
        const val TRACE_RENDER = "widget.render"
    }
}
