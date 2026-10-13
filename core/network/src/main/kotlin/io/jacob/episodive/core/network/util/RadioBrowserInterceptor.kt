package io.jacob.episodive.core.network.util

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Radio Browser 전용 인터셉터. 식별 가능한 User-Agent 를 싣고, 미러 서버를 돌아가며 시도한다.
 *
 * Podcast Index 인증 헤더를 싣는 [EpisodiveInterceptor] 와 절대 섞지 않는다.
 *
 * 미러 목록은 정적으로 둔다. 공식 안내는 `/json/servers` 로 동적 조회를 권하지만, 미러가 몇 개 안
 * 되고 거의 바뀌지 않아 목록 조회 자체가 또 하나의 실패 지점이 되는 쪽이 더 크다.
 *
 * - IOException(연결 실패·타임아웃)이나 5xx 응답은 미러 장애로 보고 다음 미러로 같은 요청을
 *   재시도한다(5xx 응답은 닫고 넘어간다). 마지막 미러까지 실패하면, 5xx 응답을 받은 적이 있으면 마지막 5xx
 *   응답을 그대로 돌려주고 전부 IOException 이었으면 마지막 예외를 던진다.
 * - 4xx 는 요청 쪽 문제라 미러를 바꿔도 같으므로 전환하지 않는다.
 * - 성공(2xx~4xx)한 미러를 기억해 다음 요청은 거기서부터 시작한다. 5xx 를 낸 미러는 기억하지 않는다.
 */
class RadioBrowserInterceptor(
    hosts: List<HttpUrl> = DEFAULT_HOSTS.map { "https://$it/".toHttpUrl() },
    private val userAgent: String = USER_AGENT,
) : Interceptor {
    private val hosts: List<HttpUrl> = hosts.also { require(it.isNotEmpty()) }

    @Volatile
    private var preferredIndex = 0

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val start = preferredIndex
        var failure: IOException? = null
        var lastServerError: Response? = null

        for (offset in hosts.indices) {
            val index = (start + offset) % hosts.size
            val base = hosts[index]
            val url = original.url.newBuilder()
                .scheme(base.scheme)
                .host(base.host)
                .port(base.port)
                .build()
            val request = original.newBuilder()
                .url(url)
                .header("User-Agent", userAgent)
                .build()

            try {
                val response = chain.proceed(request)
                if (response.code < 500) {
                    preferredIndex = index
                    lastServerError?.close()
                    return response
                }
                // 마지막 미러까지 5xx 면 이 응답을 돌려줘야 하므로, 다음 시도 전에 이전 것만 닫는다.
                lastServerError?.close()
                lastServerError = response
            } catch (e: IOException) {
                // 취소된 호출을 다른 미러로 다시 보내면 취소가 무의미해진다.
                if (chain.call().isCanceled()) {
                    lastServerError?.close()
                    throw e
                }
                failure?.let { e.addSuppressed(it) }
                failure = e
            }
        }
        // 응답을 한 번이라도 받았으면 예외보다 그 응답이 더 많은 것을 말해 준다.
        return lastServerError ?: throw failure!!
    }

    companion object {
        val DEFAULT_HOSTS = listOf(
            "de1.api.radio-browser.info",
            "de2.api.radio-browser.info",
            "fi1.api.radio-browser.info",
        )
        const val USER_AGENT = "Episodive/1.0 (Android)"
    }
}
