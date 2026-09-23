package io.jacob.episodive.core.player.audio

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

/**
 * [SpeechAudioTap] 은 오디오 스레드([SpeechAudioTap.handleBuffer])와 poll 쪽([SpeechAudioTap.poll])
 * 스레드가 갈리는 걸 전제로 설계됐지만, 이 테스트는 JVM 단일 스레드에서 둘을 순서대로 직접
 * 부른다 — 실제 동시성이 아니라 SPSC 링버퍼의 인덱스 계산·구간 경계 로직을 검증한다.
 */
class SpeechAudioTapTest {
    private val tap = SpeechAudioTap()

    /** 프레임·채널마다 다른 진폭을 쓸 수 있는 16bit LE PCM 버퍼. */
    private fun frameBuffer(
        frames: Int,
        channels: Int,
        amplitudeAt: (frame: Int, channel: Int) -> Float,
    ): ByteBuffer = ByteBuffer.allocate(frames * channels * 2).order(ByteOrder.LITTLE_ENDIAN).apply {
        repeat(frames) { frame ->
            repeat(channels) { channel ->
                val sample = (amplitudeAt(frame, channel).coerceIn(-1f, 1f) * Short.MAX_VALUE).roundToInt().toShort()
                putShort(sample)
            }
        }
        flip()
    }

    private fun mono(frames: Int, amplitudeAt: (Int) -> Float): ByteBuffer =
        frameBuffer(frames, 1) { frame, _ -> amplitudeAt(frame) }

    private fun mono(frames: Int, amplitude: Float): ByteBuffer = mono(frames) { amplitude }

    @Test
    fun `Given capture never started, When handleBuffer is fed, Then poll stays null`() {
        tap.flush(10_000, 1, C.ENCODING_PCM_16BIT)
        tap.handleBuffer(mono(10_000, 0.5f))

        assertNull(tap.poll())
    }

    @Test
    fun `Given stereo input, When handleBuffer downmixes, Then the chunk carries the channel average`() {
        tap.startCapture()
        tap.flush(10_000, 2, C.ENCODING_PCM_16BIT)
        // rate/10 = 1000 표본. 왼쪽 0.5, 오른쪽 -0.5 를 평균하면 0 이어야 한다.
        tap.handleBuffer(frameBuffer(1_000, 2) { _, channel -> if (channel == 0) 0.5f else -0.5f })

        val chunk = tap.poll()
        assertNotNull(chunk)
        chunk!!.samples.forEach { assertEquals(0f, it, 1e-4f) }
    }

    @Test
    fun `Given a known 16bit sample, When decoded, Then it scales to -1 point 1`() {
        tap.startCapture()
        tap.flush(10_000, 1, C.ENCODING_PCM_16BIT)
        tap.handleBuffer(mono(1_000, 0.25f))

        val chunk = tap.poll()
        assertNotNull(chunk)
        // Short.MAX_VALUE 로 양자화되므로 정확히 0.25f 는 아니고 그 근처다.
        chunk!!.samples.forEach { assertEquals(0.25f, it, 1e-3f) }
    }

    @Test
    fun `Given fewer than 100ms of samples, When polled, Then it returns null until the chunk fills`() {
        tap.startCapture()
        tap.flush(10_000, 1, C.ENCODING_PCM_16BIT)
        tap.handleBuffer(mono(999, 0.1f))

        assertNull(tap.poll())

        tap.handleBuffer(mono(1, 0.1f))
        assertNotNull(tap.poll())
    }

    @Test
    fun `Given a flush mid-utterance, When the rate changes, Then old samples are discarded and a new segment starts`() {
        tap.startCapture()
        tap.flush(10_000, 1, C.ENCODING_PCM_16BIT)
        tap.handleBuffer(mono(700, 0.1f)) // 100ms(1000 표본) 에 못 미친다

        assertNull(tap.poll())

        tap.flush(16_000, 1, C.ENCODING_PCM_16BIT) // rate/10 = 1600 표본
        tap.handleBuffer(mono(1_600, 0.2f))

        val chunk = tap.poll()
        assertNotNull(chunk)
        assertEquals(16_000, chunk!!.sampleRateHz)
        assertEquals(1_600, chunk.samples.size)
        // flush 전 700 표본이 앞에 섞여 있었다면 0.1f 값이 청크 앞부분에 남아 있어야 한다.
        chunk.samples.forEach { assertEquals(0.2f, it, 1e-3f) }
    }

    @Test
    fun `Given non-16bit encoding, When fed, Then the samples are ignored`() {
        tap.startCapture()
        tap.flush(10_000, 1, C.ENCODING_PCM_FLOAT)
        tap.handleBuffer(mono(2_000, 0.9f))

        assertNull(tap.poll())

        tap.flush(10_000, 1, C.ENCODING_PCM_16BIT)
        tap.handleBuffer(mono(1_000, 0.2f))

        val chunk = tap.poll()
        assertNotNull(chunk)
        chunk!!.samples.forEach { assertEquals(0.2f, it, 1e-3f) }
    }

    @Test
    fun `Given stop then start, When fed again, Then no residue from before the stop leaks in`() {
        tap.startCapture()
        tap.flush(10_000, 1, C.ENCODING_PCM_16BIT)
        tap.handleBuffer(mono(700, 0.9f)) // 정지 전 700 표본, 청크를 채우지 못한 채로 남는다

        tap.stopCapture()
        assertNull(tap.poll())

        tap.startCapture()
        tap.handleBuffer(mono(1_000, -0.4f))

        val chunk = tap.poll()
        assertNotNull(chunk)
        chunk!!.samples.forEach { assertEquals(-0.4f, it, 1e-3f) }
    }

    @Test
    fun `Given the ring buffer overruns, When polled, Then the new segment excludes the pre-overrun samples`() {
        tap.startCapture()
        tap.flush(10_000, 1, C.ENCODING_PCM_16BIT)

        // 링 크기(1 shl 19 = 524288 표본)를 한 번의 handleBuffer 호출로 넘겨, poll 을 부르지
        // 않은 채로 오버런을 강제한다 — consumedCursor 가 시작점에 그대로 있으므로 정확히 이
        // 크기를 넘는 순간 오버런 조건(cursor - consumed >= RingSize)에 걸린다.
        val ringSize = 1 shl 19
        tap.handleBuffer(mono(ringSize + 1_000) { frame -> if (frame < ringSize) 0.1f else 0.6f })

        val chunk = tap.poll()
        assertNotNull(chunk)
        assertEquals(1_000, chunk!!.samples.size)
        chunk.samples.forEach { assertEquals(0.6f, it, 1e-3f) }
    }

    @Test
    fun `Given continuous capture, When the ring wraps around, Then reads stay correct across the wrap`() {
        tap.startCapture()
        tap.flush(10_000, 1, C.ENCODING_PCM_16BIT)

        // 청크 530개(53만 표본)를 링 크기(524288 표본)보다 많이 흘려 인덱스가 최소 한 번은
        // 감싸고 돌게 만든다. 매번 채우자마자 바로 비워 오버런은 일으키지 않는다. 530번째
        // 근처(524000~524999 표본)가 정확히 그 경계를 가로지르는 청크다.
        for (i in 0 until 530) {
            val value = (i % 100) / 200f
            tap.handleBuffer(mono(1_000, value))
            val chunk = tap.poll()
            assertNotNull("chunk $i 는 채워졌어야 한다", chunk)
            chunk!!.samples.forEach { assertEquals("chunk $i 값이 어긋났다", value, it, 1e-3f) }
        }
    }

    @Test
    fun `Given a chunk spans two segments worth of leftover, When flushed exactly at the boundary, Then no mixing happens`() {
        tap.startCapture()
        tap.flush(10_000, 1, C.ENCODING_PCM_16BIT)
        tap.handleBuffer(mono(999, 0.1f)) // 한 표본만 모자란다

        assertNull(tap.poll())

        // 레이트를 그대로 두고 flush 만 해도 구간은 끊긴다(시크·EOS 시나리오).
        tap.flush(10_000, 1, C.ENCODING_PCM_16BIT)
        tap.handleBuffer(mono(1_000, 0.3f))

        val chunk = tap.poll()
        assertNotNull(chunk)
        assertEquals(1_000, chunk!!.samples.size)
        chunk.samples.forEach { assertEquals(0.3f, it, 1e-3f) }
    }

    @Test
    fun `Given startCapture and flush race on different threads, When both allocate the next id, Then every call gets a unique id`() {
        // startCapture 는 자막 스레드, flush 는 오디오 스레드에서 불린다. 예전에는 각자
        // segmentRef.get().id + 1 로 다음 id 를 계산해 set 했는데, 그러면 두 스레드가 동시에
        // 같은 현재 id 를 읽어 서로 다른 구간에 같은 id 를 매길 수 있었다 — 엔진이 레이트가
        // 바뀐 구간을 "안 바뀌었다" 고 오판해 같은 sherpa stream 에 다른 레이트를 넣게 된다.
        // 전역 AtomicInteger 채번으로 고쳤으니, 두 스레드가 barrier 로 동시에 부딪혀도
        // 호출 횟수만큼 카운터가 정확히 늘어야 한다(값을 잃어버리는 레이스가 없어야 한다).
        val iterations = 5_000
        val barrier = CyclicBarrier(2)

        val generatorField = SpeechAudioTap::class.java.getDeclaredField("segmentIdGenerator")
        generatorField.isAccessible = true
        val generator = generatorField.get(tap) as AtomicInteger
        val startId = generator.get()

        val captureThread = Thread {
            repeat(iterations) {
                barrier.await()
                tap.startCapture()
            }
        }
        val flushThread = Thread {
            repeat(iterations) {
                barrier.await()
                tap.flush(10_000, 1, C.ENCODING_PCM_16BIT)
            }
        }

        captureThread.start()
        flushThread.start()
        captureThread.join()
        flushThread.join()

        assertEquals(startId + iterations * 2, generator.get())
    }
}
