package io.jacob.episodive.core.data.util

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageCacheInterceptorTest {

    private val interceptor = ImageCacheInterceptor()

    @Test
    fun `Given HTTP URL, when intercept called, then converts to HTTPS`() {
        // Given
        val httpUrl = "http://example.com/image.jpg"
        val originalRequest = Request.Builder()
            .url(httpUrl)
            .build()

        val chain = mockk<Interceptor.Chain>()
        val response = mockk<Response>(relaxed = true)

        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } returns response

        // When
        interceptor.intercept(chain)

        // Then
        verify {
            chain.proceed(match { request ->
                request.url.scheme == "https" &&
                        request.header("User-Agent") == "Episodive/1.0"
            })
        }
    }

    @Test
    fun `Given HTTPS URL, when intercept called, then keeps HTTPS`() {
        // Given
        val httpsUrl = "https://example.com/image.jpg"
        val originalRequest = Request.Builder()
            .url(httpsUrl)
            .build()

        val chain = mockk<Interceptor.Chain>()
        val response = mockk<Response>(relaxed = true)

        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } returns response

        // When
        interceptor.intercept(chain)

        // Then
        verify {
            chain.proceed(match { request ->
                request.url.scheme == "https"
            })
        }
    }

    @Test
    fun `Given pexels URL, when intercept called, then adds cache headers`() {
        // Given
        val pexelsUrl = "https://images.pexels.com/photos/123/photo.jpg"
        val originalRequest = Request.Builder()
            .url(pexelsUrl)
            .build()

        val chain = mockk<Interceptor.Chain>()
        val originalResponse = mockk<Response>(relaxed = true)
        val modifiedResponse = mockk<Response>(relaxed = true)

        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } returns originalResponse
        every { originalResponse.newBuilder() } returns mockk {
            every { header(any(), any()) } returns this
            every { removeHeader(any()) } returns this
            every { build() } returns modifiedResponse
        }

        // When
        val result = interceptor.intercept(chain)

        // Then
        verify {
            originalResponse.newBuilder()
        }
        assertEquals(modifiedResponse, result)
    }

    @Test
    fun `Given non-pexels URL, when intercept called, then returns response as is`() {
        // Given
        val normalUrl = "https://example.com/image.jpg"
        val originalRequest = Request.Builder()
            .url(normalUrl)
            .build()

        val chain = mockk<Interceptor.Chain>()
        val response = mockk<Response>(relaxed = true)

        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } returns response

        // When
        val result = interceptor.intercept(chain)

        // Then
        assertEquals(response, result)
    }
}