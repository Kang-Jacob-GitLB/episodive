package io.jacob.episodive.feature.widget.image

import android.graphics.Bitmap
import android.graphics.Color
import io.jacob.episodive.core.designsystem.component.EpisodiveDominantColor

/**
 * 썸네일에서 위젯 배경색을 추출한다.
 *
 * Glance 는 런타임 그라데이션을 지원하지 않으므로 단색 배경 + 스크림 조합을 쓴다.
 * 색 추출 자체는 앱 전체가 공유하는 [EpisodiveDominantColor] 가 담당한다 — 위젯과 앱 화면이
 * 같은 커버에서 같은 색을 내야 하므로 여기에 별도 규칙을 두지 않는다.
 */
object WidgetColorExtractor {
    /** 비트맵이 없거나 추출 실패 시 기본 다크 뉴트럴. */
    const val FALLBACK: Int = EpisodiveDominantColor.FALLBACK_ARGB

    /** "나의 최신 피드" 영역 배경을 만들 때 now-playing 배경에 곱하는 추가 어둡기 계수. */
    private const val FEED_DARKEN_FACTOR = 0.68

    /** now-playing 배경색과 그로부터 파생한 피드 영역 배경색 묶음. */
    data class WidgetBackground(val background: Int, val feed: Int)

    /**
     * 썸네일에서 배경색과 피드 배경색을 한 번에 산출하는 단일 진입점.
     * Palette 추출은 한 번만 수행([backgroundColor])하고, 피드색은 그 결과에서 파생한다.
     * 호출부가 두 함수를 따로 엮을 필요 없이 한 패스로 두 색을 받는다.
     */
    fun colors(bitmap: Bitmap?): WidgetBackground = fromColor(backgroundColor(bitmap))

    /** 이미 정해진 배경색에서 피드 영역 색을 파생한다. 추출을 거치지 않는 경로(스테이션)가 쓴다. */
    fun fromColor(background: Int): WidgetBackground =
        WidgetBackground(background, feedBackgroundColor(background))

    fun backgroundColor(bitmap: Bitmap?): Int {
        if (bitmap == null) return FALLBACK
        return EpisodiveDominantColor.extractArgb(bitmap) ?: FALLBACK
    }

    /**
     * now-playing 배경색([backgroundColor])을 한 단계 더 어둡게 한 피드 영역 배경색.
     * 같은 색조를 유지하면서 톤만 낮춰 단일 위젯 내 영역 구분을 준다.
     */
    fun feedBackgroundColor(backgroundColor: Int): Int {
        val r = (Color.red(backgroundColor) * FEED_DARKEN_FACTOR).toInt()
        val g = (Color.green(backgroundColor) * FEED_DARKEN_FACTOR).toInt()
        val b = (Color.blue(backgroundColor) * FEED_DARKEN_FACTOR).toInt()
        return Color.rgb(r, g, b)
    }
}
