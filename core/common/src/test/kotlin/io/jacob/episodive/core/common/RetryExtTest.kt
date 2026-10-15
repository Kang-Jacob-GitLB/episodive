package io.jacob.episodive.core.common

import app.cash.turbine.test
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class RetryExtTest {
    @Test
    fun `Given IOException 이 두 번, Then 1초 2초 뒤에 다시 구독해 복구하고 실패마다 대체 값을 흘린다`() = runTest {
        var attempt = 0
        val source = flow {
            if (attempt++ < 2) throw IOException("offline")
            emit("ok")
        }

        source.retryNetworkWithBackoff { emit("fallback") }.test {
            assertEquals("fallback", awaitItem())
            assertEquals(0L, testScheduler.currentTime)
            assertEquals("fallback", awaitItem())
            assertEquals(1_000L, testScheduler.currentTime)
            assertEquals("ok", awaitItem())
            assertEquals(3_000L, testScheduler.currentTime)
            awaitComplete()
        }
    }

    @Test
    fun `Given 대기가 계속 늘어남, Then 최대 대기에서 멈춘다`() = runTest {
        var attempt = 0
        val source = flow<String> {
            if (attempt++ < 8) throw IOException("offline")
            emit("ok")
        }

        source.retryNetworkWithBackoff(baseMillis = 1_000, maxMillis = 4_000).test {
            assertEquals("ok", awaitItem())
            // 1 + 2 + 4 + 4 + 4 + 4 + 4 + 4
            assertEquals(27_000L, testScheduler.currentTime)
            awaitComplete()
        }
    }

    @Test
    fun `Given 값이 온 뒤 다시 실패, Then 대기는 처음부터 다시 시작한다`() = runTest {
        var attempt = 0
        val source = flow {
            when (attempt++) {
                0, 1 -> throw IOException("offline")
                2 -> {
                    emit("a")
                    throw IOException("dropped")
                }
                else -> emit("b")
            }
        }

        source.retryNetworkWithBackoff().test {
            assertEquals("a", awaitItem())
            assertEquals(3_000L, testScheduler.currentTime)
            // 값이 왔으니 대기가 1초로 되돌아가 있다(되돌아가지 않았다면 4초).
            assertEquals("b", awaitItem())
            assertEquals(4_000L, testScheduler.currentTime)
            awaitComplete()
        }
    }

    @Test
    fun `Given IOException 이 아닌 실패, Then 재시도 없이 대체 값 한 번 뒤 예외 없이 끝난다`() = runTest {
        var attempt = 0
        val source = flow<String> {
            attempt++
            throw IllegalStateException("boom")
        }

        source.retryNetworkWithBackoff { emit("fallback") }.test {
            assertEquals("fallback", awaitItem())
            awaitComplete()
        }
        assertEquals(1, attempt)
    }
}
