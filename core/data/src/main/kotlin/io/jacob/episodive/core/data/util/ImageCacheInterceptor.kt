package io.jacob.episodive.core.data.util

import okhttp3.Interceptor
import okhttp3.Response

class ImageCacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // HTTP를 HTTPS로 변경 + User-Agent 헤더 추가
        val originalRequest = chain.request()
        val httpsUrl = if (originalRequest.url.scheme == "http") {
            originalRequest.url.newBuilder().scheme("https").build()
        } else {
            originalRequest.url
        }

        val request = originalRequest.newBuilder()
            .url(httpsUrl)
            .addHeader("User-Agent", "Episodive/1.0")
            .build()

        val response = chain.proceed(request)

        // https://images.pexels.com으로 시작하는 URL은 캐시를 영구적으로 저장
        if (request.url.host.contains("images.pexels.com")) {
            return response.newBuilder()
                .header("Cache-Control", "public, max-age=31536000, immutable") // 1년
                .removeHeader("Expires")
                .removeHeader("Pragma")
                .build()
        }

        return response
    }
}