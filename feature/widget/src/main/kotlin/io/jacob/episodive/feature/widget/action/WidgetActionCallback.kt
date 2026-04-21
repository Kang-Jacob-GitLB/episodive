package io.jacob.episodive.feature.widget.action

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.tracing.trace
import io.jacob.episodive.feature.widget.PlaybackControl

/**
 * 위젯 버튼 클릭을 ServiceActions 의 PendingIntent 로 위임한다.
 * `androidx.glance` 는 `PendingIntent` 를 직접 붙이는 API 가 없으므로,
 * ActionCallback 을 통해 `PendingIntent.send()` 를 호출한다.
 *
 * cold-start 여부에 따라 서비스 vs MainActivity+autoplay 로 자동 분기
 * (ServiceActions 내부 `isMediaServiceRunning` 검사).
 */
class WidgetActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val control = parameters[PlaybackControl.KEY] ?: return
        trace("widget.click") {
            when (control) {
                PlaybackControl.PLAY_PAUSE.name ->
                    playPausePendingIntent(context, REQ_PLAY_PAUSE).send()
                PlaybackControl.SEEK_FWD.name ->
                    seekForwardPendingIntent(context, REQ_SEEK_FWD).send()
                PlaybackControl.SEEK_BWD.name ->
                    seekBackwardPendingIntent(context, REQ_SEEK_BWD).send()
            }
        }
    }

    companion object {
        private const val REQ_PLAY_PAUSE = 1
        private const val REQ_SEEK_FWD = 2
        private const val REQ_SEEK_BWD = 3
    }
}
