package io.jacob.episodive.feature.widget.component

/**
 * 스테이션 이니셜 썸네일의 글자 크기(sp 값).
 *
 * Glance 의 `sp` 는 기기 글꼴 배율을 따라 커져 정사각형 상자를 넘친다. 상자는 dp 로 고정이니 글자도 dp 로
 * 고정해야 해서, 배율로 나눠 상쇄한다(`sp * fontScale` = 원하는 dp). 비선형 글꼴 배율(Android 14+)에서는
 * 근사치다.
 *
 * @param sizeDp 정사각형 한 변
 * @param textRatio 한 변 대비 글자 비율(`StationArt.textRatio`)
 */
internal fun stationInitialsFontSizeSp(sizeDp: Int, textRatio: Float, fontScale: Float): Float =
    sizeDp * textRatio / fontScale.coerceAtLeast(MinFontScale)

private const val MinFontScale = 0.1f
