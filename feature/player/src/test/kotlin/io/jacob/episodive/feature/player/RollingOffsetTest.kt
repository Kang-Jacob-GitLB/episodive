package io.jacob.episodive.feature.player

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [RollingOffset] 은 측정 단계에서 즉시 점프값을 대입하고, 0 으로 되돌아가는 애니메이션은 별도
 * 코루틴으로 띄운다(`RollingCaption.kt` 문서 참고). 여기서는 스케줄러를 절대 진행시키지 않는
 * [TestScope]/[StandardTestDispatcher] 를 써서 그 launch 된 애니메이션이 한 스텝도 돌지 않게
 * 만들고, `onMeasured` 가 동기적으로 대입하는 점프값만 관찰한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RollingOffsetTest {
    private val scope = TestScope(StandardTestDispatcher())

    @Test
    fun `first measure leaves the offset at zero`() {
        val rolling = RollingOffset(scope)

        rolling.onMeasured(mapOf(1L to 40))

        assertEquals(0f, rolling.offset)
    }

    @Test
    fun `appending a new line below jumps the offset by the anchor's added distance`() {
        val rolling = RollingOffset(scope)
        rolling.onMeasured(mapOf(1L to 20, 2L to 0)) // 두 줄, 각 줄 높이 20

        // 아래에 새 줄이 붙어 앵커(2L)가 바닥에서 20 만큼 멀어졌다.
        rolling.onMeasured(mapOf(1L to 40, 2L to 20, 3L to 0))

        assertEquals(20f, rolling.offset)
    }

    @Test
    fun `dropping the top line while nothing else changes leaves the offset unchanged`() {
        val rolling = RollingOffset(scope)
        rolling.onMeasured(mapOf(1L to 40, 2L to 20, 3L to 0))

        // 맨 윗줄(1L)만 버려지고 남은 줄들의 바닥 기준 거리는 그대로다.
        rolling.onMeasured(mapOf(2L to 20, 3L to 0))

        assertEquals(0f, rolling.offset)
    }

    @Test
    fun `dropping the top line while appending a new line jumps by only the height added below`() {
        // 예전 설계의 회귀 지점 — "콘텐츠 전체 높이가 늘면 애니메이션" 으로 재면 버려진 윗줄만큼
        // 상쇄돼 새 줄이 제자리에서 툭 바뀌었다. 앵커 기준으로 재면 아래에 더해진 높이만큼만 움직인다.
        val rolling = RollingOffset(scope)
        rolling.onMeasured(mapOf(1L to 40, 2L to 20, 3L to 0)) // 세 줄, 각 높이 20

        // 맨 윗줄(1L)은 버려지고 바닥에 새 줄(4L)이 붙는다.
        rolling.onMeasured(mapOf(2L to 40, 3L to 20, 4L to 0))

        assertEquals(20f, rolling.offset)
    }

    @Test
    fun `no common id resets the offset to zero`() {
        val rolling = RollingOffset(scope)
        rolling.onMeasured(mapOf(1L to 20, 2L to 0))

        // 구간이 바뀌어 이전 id 가 하나도 남지 않았다(새 세션·새 발화).
        rolling.onMeasured(mapOf(3L to 20, 4L to 0))

        assertEquals(0f, rolling.offset)
    }

    @Test
    fun `a partial line growing in place jumps the offset by its growth`() {
        val rolling = RollingOffset(scope)
        rolling.onMeasured(mapOf(1L to 20)) // 한 줄, 높이 20

        // 같은 줄(1L)이 두 줄로 줄바꿈되며 10 만큼 자란다.
        rolling.onMeasured(mapOf(1L to 30))

        assertEquals(10f, rolling.offset)
    }
}
