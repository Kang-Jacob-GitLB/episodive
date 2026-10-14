package io.jacob.episodive.core.network.datasource

import com.google.gson.Gson
import io.jacob.episodive.core.network.model.BroadcasterSource
import io.jacob.episodive.core.network.model.KbsLiveResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import timber.log.Timber
import java.io.IOException

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
            is BroadcasterSource.Kbs -> gson.fromJson(body, KbsLiveResponse::class.java)
                ?.channelItem?.firstOrNull()?.serviceUrl
            is BroadcasterSource.Mbc,
            is BroadcasterSource.Sbs -> body
        }
        url?.trim()?.takeIf { it.startsWith("https://") }
    }

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

    private fun fetch(url: HttpUrl): String {
        val request = Request.Builder().url(url).build()
        callFactory.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}: $url")
            return response.body.string()
        }
    }
}
