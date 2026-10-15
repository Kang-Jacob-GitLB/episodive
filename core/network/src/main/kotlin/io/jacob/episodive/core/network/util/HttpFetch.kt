package io.jacob.episodive.core.network.util

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resumeWithException

/** 코루틴이 취소되면 진행 중인 HTTP 호출도 함께 취소한다. 2xx 가 아니면 [IOException]. */
internal suspend fun Call.Factory.fetchBody(request: Request): String =
    suspendCancellableCoroutine { continuation ->
        val call = newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val result = runCatching {
                    response.use {
                        if (!it.isSuccessful) throw IOException("HTTP ${it.code}: ${request.url}")
                        it.body.string()
                    }
                }
                continuation.resumeWith(result)
            }
        })
    }
