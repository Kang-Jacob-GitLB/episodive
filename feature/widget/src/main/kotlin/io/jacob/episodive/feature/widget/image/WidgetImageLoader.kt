package io.jacob.episodive.feature.widget.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale

object WidgetImageLoader {
    private const val TAG = "WidgetPerf"
    private const val DEFAULT_SIZE_PX = 256

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
        if (url.isNullOrBlank()) {
            Log.d(TAG, "loadWidgetBitmap skip: url null/blank")
            return null
        }
        return runCatching {
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(sizePx)
                .scale(Scale.FILL)
                .allowHardware(false)
                .build()
            when (val result = ImageLoader(context).execute(request)) {
                is SuccessResult -> {
                    val bmp = (result.drawable as? BitmapDrawable)?.bitmap
                    Log.d(TAG, "loadWidgetBitmap OK url=$url bitmap=${bmp != null}")
                    bmp
                }
                is ErrorResult -> {
                    Log.w(TAG, "loadWidgetBitmap ERROR url=$url", result.throwable)
                    null
                }
            }
        }.getOrElse { e ->
            Log.e(TAG, "loadWidgetBitmap threw for url=$url", e)
            null
        }
    }
}
