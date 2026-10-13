package io.jacob.episodive.core.player.datasource

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import io.jacob.episodive.core.model.StationArt
import java.io.ByteArrayOutputStream

/**
 * 알림·잠금화면용 스테이션 이니셜 아트를 PNG 바이트로 그린다. 앱 안의 Compose 구현과 같은
 * 규칙([StationArt])·글자 비율을 쓴다. 글꼴은 시스템 기본 Bold 라 한글도 그려진다.
 *
 * 메인 스레드에서 도는 `toMediaItem` 이 부르므로 결과는 호출 측이 스테이션 id 로 캐시한다.
 */
internal object StationArtworkRenderer {

    fun render(art: StationArt, sizePx: Int = DefaultSizePx): ByteArray {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.drawColor(art.colorArgb)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
                textSize = sizePx * art.textRatio
            }
            // 글자 중심(ascent·descent 의 가운데)을 정사각형 중심에 맞춘다.
            val baseline = sizePx / 2f - (paint.ascent() + paint.descent()) / 2f
            canvas.drawText(art.initials, sizePx / 2f, baseline, paint)

            return ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    const val DefaultSizePx = 320
}
