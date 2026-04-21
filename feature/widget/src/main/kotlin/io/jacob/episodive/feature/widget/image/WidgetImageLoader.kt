package io.jacob.episodive.feature.widget.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Scale

object WidgetImageLoader {
    /**
     * Coil 을 통해 위젯 썸네일 Bitmap 을 로드한다.
     * - `allowHardware(false)` 로 RemoteViews 직렬화 호환 보장
     * - 최대 sizePx 로 제한해 1MB RemoteViews 한도 회피
     */
    suspend fun loadWidgetBitmap(
        context: Context,
        url: String?,
        sizePx: Int = DEFAULT_SIZE_PX,
    ): Bitmap? {
        if (url.isNullOrBlank()) return null
        return runCatching {
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(sizePx)
                .scale(Scale.FILL)
                .allowHardware(false)
                .build()
            val result = ImageLoader(context).execute(request)
            (result.drawable as? BitmapDrawable)?.bitmap
        }.getOrNull()
    }

    private const val DEFAULT_SIZE_PX = 256
}
