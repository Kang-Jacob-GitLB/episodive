package io.jacob.episodive.core.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import java.io.IOException

/**
 * 네트워크 실패([IOException])만 백오프로 다시 구독한다. 대기는 [baseMillis] 에서 시작해 두 배씩 늘어 [maxMillis] 에서
 * 멈추고, 값이 한 번이라도 오면 처음으로 되돌아간다(오래 정상이던 흐름이 끊겼을 때 긴 대기부터 시작하지 않는다).
 *
 * 실패할 때마다 재시도에 앞서 [onFailure] 를 부른다 — 재시도 사이에 내보낼 대체 값(빈 목록 등)을 여기서 흘린다.
 * [IOException] 이 아닌 실패는 [onFailure] 를 한 번 부른 뒤 **예외를 올리지 않고 흐름을 끝낸다.** 재시도해도
 * 나아지지 않는 실패이고, 호출한 쪽은 대체 값으로 이미 그릴 수 있기 때문이다. 취소는 그대로 전파된다.
 */
fun <T> Flow<T>.retryNetworkWithBackoff(
    baseMillis: Long = 1_000L,
    maxMillis: Long = 30_000L,
    onFailure: suspend FlowCollector<T>.(cause: Throwable) -> Unit = {},
): Flow<T> = flow {
    var failures = 0
    emitAll(
        this@retryNetworkWithBackoff
            .onEach { failures = 0 }
            .retryWhen { cause, _ ->
                onFailure(cause)
                if (cause !is IOException) return@retryWhen false
                delay((baseMillis shl failures.coerceAtMost(MaxShift)).coerceAtMost(maxMillis))
                failures++
                true
            }
            .catch { /* IOException 이 아닌 실패. onFailure 로 알렸으므로 여기서 끝낸다. */ },
    )
}

private const val MaxShift = 10
