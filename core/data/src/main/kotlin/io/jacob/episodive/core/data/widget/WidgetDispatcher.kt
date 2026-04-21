package io.jacob.episodive.core.data.widget

import android.content.Context

/**
 * WidgetUpdater 가 실제 GlanceAppWidget.updateAll 을 호출하는 지점을 역의존으로 분리한다.
 * `:core:data` → `:feature:widget` 의존을 금지하기 위해 인터페이스만 여기에 두고
 * 구현은 `:feature:widget` 의 GlanceWidgetDispatcher 가 Hilt @Binds 로 제공한다.
 */
fun interface WidgetDispatcher {
    suspend fun dispatch(context: Context, request: WidgetUpdateRequest)
}
