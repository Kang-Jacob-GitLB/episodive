package io.jacob.episodive.feature.widget

import androidx.glance.action.ActionParameters

/**
 * 위젯 Action 파라미터 키 — 버튼 종류 구분용.
 * entries.find 패턴 준수 (valueOf 금지).
 */
enum class PlaybackControl {
    PLAY_PAUSE,
    SEEK_FWD,
    SEEK_BWD;

    companion object {
        val KEY: ActionParameters.Key<String> = ActionParameters.Key("widget_playback_control")

        fun fromName(name: String?): PlaybackControl? =
            entries.find { it.name == name }
    }
}
