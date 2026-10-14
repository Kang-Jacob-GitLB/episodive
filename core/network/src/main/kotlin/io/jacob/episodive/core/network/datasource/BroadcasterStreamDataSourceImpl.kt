package io.jacob.episodive.core.network.datasource

import com.google.gson.Gson
import io.jacob.episodive.core.network.model.BroadcasterSource
import io.jacob.episodive.core.network.model.KbsLiveResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import kotlin.coroutines.resumeWithException

/** 방송사별 호스트. 테스트에서 MockWebServer 로 돌리기 위해 갈아끼울 수 있게 뒀다. */
data class BroadcasterEndpoints(
    val kbs: HttpUrl = "https://cfpwwwapi.kbs.co.kr/".toHttpUrl(),
    val mbc: HttpUrl = "https://sminiplay.imbc.com/".toHttpUrl(),
    val sbs: HttpUrl = "https://apis.sbs.co.kr/".toHttpUrl(),
)

class BroadcasterStreamDataSourceImpl(
    private val callFactory: Call.Factory,
    private val gson: Gson,
    private val ioDispatcher: CoroutineDispatcher,
    private val endpoints: BroadcasterEndpoints = BroadcasterEndpoints(),
) : BroadcasterStreamDataSource {

    override suspend fun resolve(source: BroadcasterSource): String? = withContext(ioDispatcher) {
        Timber.i("resolve source: $source")
        val body = fetch(source.toUrl())
        val url = when (source) {
            // channel_item 에는 라디오 음원(radio)과 보이는 라디오 영상(bora)이 함께 온다.
            is BroadcasterSource.Kbs -> gson.fromJson(body, KbsLiveResponse::class.java)
                ?.channelItem
                ?.filter { it.mediaType == "radio" }
                ?.firstNotNullOfOrNull { it.serviceUrl?.trim()?.takeIf(::isHttps) }
            is BroadcasterSource.Mbc,
            is BroadcasterSource.Sbs -> body.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
        }
        url?.takeIf(::isHttps)
    }

    private fun isHttps(url: String) = url.startsWith("https://")

    private fun BroadcasterSource.toUrl(): HttpUrl = when (this) {
        is BroadcasterSource.Kbs -> endpoints.kbs.newBuilder()
            .addPathSegments("api/v1/landing/live/channel_code")
            .addPathSegment(channelCode)
            .build()
        is BroadcasterSource.Mbc -> endpoints.mbc.newBuilder()
            .addPathSegment("aacplay.ashx")
            .addQueryParameter("agent", "webapp")
            .addQueryParameter("channel", channel)
            .build()
        is BroadcasterSource.Sbs -> endpoints.sbs.newBuilder()
            .addPathSegments("play-api/1.0/livestream")
            .addPathSegments(path)
            .addQueryParameter("protocol", "hls")
            .addQueryParameter("ssl", "Y")
            .build()
    }

    /** 코루틴이 취소되면 진행 중인 HTTP 호출도 함께 취소한다. */
    private suspend fun fetch(url: HttpUrl): String = suspendCancellableCoroutine { continuation ->
        val call = callFactory.newCall(Request.Builder().url(url).build())
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val result = runCatching {
                    response.use {
                        if (!it.isSuccessful) throw IOException("HTTP ${it.code}: $url")
                        it.body.string()
                    }
                }
                continuation.resumeWith(result)
            }
        })
    }
}
