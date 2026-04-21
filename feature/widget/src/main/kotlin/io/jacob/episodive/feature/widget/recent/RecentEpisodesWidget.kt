package io.jacob.episodive.feature.widget.recent

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.tracing.trace
import dagger.hilt.android.EntryPointAccessors
import io.jacob.episodive.core.domain.widget.EpisodeSnapshot
import io.jacob.episodive.core.domain.widget.WidgetDataReaderEntryPoint
import io.jacob.episodive.feature.widget.image.WidgetImageLoader
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * 최근 에피소드 위젯.
 *
 * 규약:
 * - `provideGlance` 내부에서는 `Flow.first()` 스냅샷만 사용. 장기 `collect` 금지.
 * - 재렌더 트리거는 외부의 `WidgetUpdater` → `WidgetDispatcher` → `updateAll()` 경로로만 발생.
 * - tracing: `widget.render` 구간 + `WidgetPerf` Logcat 태그.
 */
class RecentEpisodesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        trace(TRACE_RENDER) {
            val startMs = SystemClock.uptimeMillis()
            val reader = EntryPointAccessors
                .fromApplication(
                    context.applicationContext,
                    WidgetDataReaderEntryPoint::class.java,
                )
                .widgetDataReader()
            val snapshots = runCatching { reader.snapshotRecentEpisodes(LIMIT) }
                .onFailure { Log.e(TAG, "snapshotRecentEpisodes failed", it) }
                .getOrDefault(emptyList())
            val artworks = loadArtworks(context, snapshots)

            provideContent {
                GlanceTheme {
                    RecentEpisodesContent(snapshots, artworks)
                }
            }

            val deltaMs = SystemClock.uptimeMillis() - startMs
            Log.d(
                PERF_TAG,
                "render count=${snapshots.size} bitmaps=${artworks.count { it.value != null }} deltaMs=$deltaMs",
            )
        }
    }

    /**
     * 썸네일을 `async` 로 병렬 로드해 cold-start 렌더 지연을 줄인다.
     */
    private suspend fun loadArtworks(
        context: Context,
        snapshots: List<EpisodeSnapshot>,
    ): Map<Long, Bitmap?> = coroutineScope {
        snapshots
            .map { snapshot ->
                async {
                    snapshot.id to WidgetImageLoader.loadWidgetBitmap(
                        context,
                        snapshot.imageUrl,
                        ARTWORK_PX,
                    )
                }
            }
            .awaitAll()
            .toMap()
    }

    private companion object {
        const val TAG = "RecentEpisodesWidget"
        const val PERF_TAG = "WidgetPerf"
        const val TRACE_RENDER = "widget.render"
        const val LIMIT = 5
        const val ARTWORK_PX = 128
    }
}
