package io.jacob.episodive.feature.widget.dispatcher

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.tracing.trace
import io.jacob.episodive.core.data.widget.WidgetDispatcher
import io.jacob.episodive.core.data.widget.WidgetUpdateRequest
import io.jacob.episodive.feature.widget.nowplaying.NowPlayingWidget
import io.jacob.episodive.feature.widget.recent.RecentEpisodesWidget
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `WidgetUpdater` 의 실제 `GlanceAppWidget.updateAll` 호출 구현체.
 *
 * `:core:data` 가 `:feature:widget` 에 역의존하지 않도록 인터페이스는 `:core:data` 에,
 * 구현은 여기에 두고 Hilt `@Binds` 로 연결한다 (WidgetDispatcherModule).
 */
@Singleton
class GlanceWidgetDispatcher @Inject constructor() : WidgetDispatcher {
    override suspend fun dispatch(context: Context, request: WidgetUpdateRequest) {
        trace(TRACE_UPDATE) {
            when (request) {
                is WidgetUpdateRequest.NowPlayingChanged -> {
                    NowPlayingWidget().updateAll(context)
                }
                is WidgetUpdateRequest.RecentEpisodesChanged -> {
                    RecentEpisodesWidget().updateAll(context)
                }
                is WidgetUpdateRequest.All -> {
                    NowPlayingWidget().updateAll(context)
                    RecentEpisodesWidget().updateAll(context)
                }
            }
        }
    }

    private companion object {
        const val TRACE_UPDATE = "widget.updateAll"
    }
}
