package io.jacob.episodive.core.player.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 라이브 자막(STT) 이 먹을 PCM 을 Main 플레이어의 오디오 프로세서 체인에서 엿듣는다.
 *
 * [PlaybackSpectrumMonitor] 와 같은 자리(`TeeAudioProcessor.AudioBufferSink`)에 붙지만 목적이
 * 다르다 — 저쪽은 화면에 보여줄 다섯 칸 세기만 필요해 창을 버려도 그만이지만, 이쪽은 sherpa-onnx
 * 인식기에 **끊김 없이** 넘겨야 한다. 그래서 창 하나짜리 [AtomicReference] 대신 SPSC 링버퍼로
 * 쌓아 두고 [poll] 쪽이 100ms 단위로 꺼내 간다.
 *
 * 같은 이유로 관용구는 [PlaybackSpectrumMonitor] 를 그대로 따른다: 16bit PCM 만 읽고, 채널은
 * 평균해 모노로 접고, [handleBuffer] 는 오디오 스레드에서 시간을 끌지 않는다(할당·락 금지).
 */
@Singleton
// 구현하는 인터페이스의 두 오버라이드가 media3 opt-in API 라 클래스 전체에 붙인다.
@OptIn(UnstableApi::class)
class SpeechAudioTap @Inject constructor() : TeeAudioProcessor.AudioBufferSink, SpeechPcmSource {

    /** 자막이 꺼져 있으면 [handleBuffer] 첫 줄에서 바로 반환해 비용이 0 이어야 한다. */
    @Volatile
    private var capturing = false

    /** [flush] 는 오디오 스레드 밖에서도 불릴 수 있어 [handleBuffer] 와 스레드가 갈린다. */
    @Volatile
    private var pcmEncoding: Int = C.ENCODING_PCM_16BIT

    @Volatile
    private var sampleRateHz: Int = DefaultSampleRateHz

    @Volatile
    private var channelCount: Int = 2

    /**
     * 링버퍼 본체. [startCapture] 가 처음 불릴 때 딱 한 번만 만든다 — 자막을 아예 켠 적이 없는
     * 세션에서는 512K 짜리 float 배열(2MB)을 만들 이유가 없다.
     */
    @Volatile
    private var ring: FloatArray? = null

    /** 다음에 쓸 절대 인덱스. 오디오 스레드만 늘린다(단일 생산자). */
    @Volatile
    private var writeCursor = 0L

    /**
     * 리더가 실제로 소비를 마친 절대 인덱스. [poll] 이 갱신하는 것이 정상 경로지만, 오버런·
     * [flush]·[startCapture] 때는 오디오 스레드가 직접 앞으로 당겨 "여기까지는 버렸다" 고
     * 표시한다 — 그러지 않으면 방금 끊은 구간 앞의 빈 공간이 계속 꽉 찬 것으로 보여 다음
     * 표본마다 오버런이 반복된다.
     */
    @Volatile
    private var consumedCursor = 0L

    /**
     * 지금 채우고 있는 구간. id 는 세 가지 경우에만 오른다: ① [flush](시크·EOS·리셋) ②
     * 오버런(리더가 못 따라와 링이 가득 참) ③ [startCapture]. 셋 다 "이 앞뒤 표본은 이어
     * 붙이면 안 된다" 는 뜻이라 [poll] 은 구간이 바뀌면 새 [Segment.startIndex] 부터 다시 센다.
     */
    private val segmentRef = AtomicReference(Segment(id = 0, startIndex = 0L, rateHz = DefaultSampleRateHz))

    /**
     * 구간 id 채번 전용. [startCapture](자막 스레드) 와 [flush]·오버런(오디오 스레드) 이 각자
     * `segmentRef.get().id + 1` 을 계산해 `set` 하면, 두 스레드가 동시에 같은 현재 id 를 읽어
     * 서로 다른 구간에 같은 id 를 매길 수 있다 — 엔진이 이를 "구간이 안 바뀌었다" 고 오판해
     * 레이트가 다른 표본을 같은 sherpa stream 에 넣다 네이티브가 죽는다. id 는 이 카운터
     * 하나로만 채번해 전역으로 단조 증가시킨다.
     */
    private val segmentIdGenerator = AtomicInteger(0)

    // ----- poll() 전용 (단일 소비자이므로 락 없이 이 필드들만 만진다)

    private var pollSegmentId = -1
    private var readCursor = 0L

    /** 레이트별로 재사용하는 청크 배열. sherpa `acceptWaveform` 이 배열 전체를 받으므로 매번
     * 새로 만들지 않고 레이트가 같으면 그대로 덮어써 돌려준다. */
    private val chunkBuffers = HashMap<Int, FloatArray>()

    override fun startCapture() {
        if (ring == null) ring = FloatArray(RingSize)
        val start = writeCursor
        val next = Segment(id = segmentIdGenerator.incrementAndGet(), startIndex = start, rateHz = sampleRateHz)
        segmentRef.set(next)
        // 리더 커서도 새 시작점으로 당겨 둔다. 그러지 않으면 정지해 있던 동안 실제로는 아무도
        // 읽지 않은 구간이 "밀린 것" 으로 계산돼 재개하자마자 오버런으로 오판한다.
        consumedCursor = start
        // capturing 을 마지막에 켠다. 이 대입이 volatile write 라, 그 전의 ring/segmentRef/
        // consumedCursor 갱신이 이 값을 true 로 읽는 오디오 스레드에도 함께 보인다(happens-before).
        capturing = true
    }

    override fun stopCapture() {
        capturing = false
    }

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        pcmEncoding = encoding
        this.sampleRateHz = sampleRateHz
        this.channelCount = channelCount
        // 오디오 스레드에서 불린다(시크·EOS·리셋). 지금까지 쌓인 표본은 다음에 올 표본과
        // 다른 시각·어쩌면 다른 레이트라 이어 붙이면 안 된다 — 구간을 끊는다.
        segmentRef.set(Segment(id = segmentIdGenerator.incrementAndGet(), startIndex = writeCursor, rateHz = sampleRateHz))
        consumedCursor = writeCursor
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        if (!capturing) return
        // 16bit PCM 만 읽는다. float 출력은 기본으로 꺼져 있다.
        if (pcmEncoding != C.ENCODING_PCM_16BIT) return

        val buf = ring ?: return

        val rate = sampleRateHz
        val channels = channelCount.coerceAtLeast(1)
        val frameBytes = channels * BytesPerSample

        // `position` 을 건드리지 않도록 절대 인덱스로 읽는다.
        var index = buffer.position()
        val end = buffer.limit()

        var seg = segmentRef.get()
        var cursor = writeCursor
        var consumed = consumedCursor

        while (index + frameBytes <= end) {
            // 채널을 평균해 모노로 접는다. 표본을 건너뛰며 읽으면 유효 Nyquist 가 떨어져
            // 음성 인식에 필요한 고역 정보가 저역 자리로 접혀 들어온다.
            var sum = 0f
            var offset = index
            repeat(channels) {
                // 16bit PCM 은 리틀엔디안이다. 버퍼의 바이트 순서 설정에 기대지 않고 직접 맞춘다.
                val sample = (buffer.get(offset + 1).toInt() shl 8) or
                        (buffer.get(offset).toInt() and 0xFF)
                sum += sample / Short.MAX_VALUE.toFloat()
                offset += BytesPerSample
            }
            index += frameBytes

            if (cursor - consumed >= RingSize) {
                // 오버런: 리더가 못 따라왔다. 링을 덮어써 이어 붙이는 대신, 지금 지점에서
                // 구간을 끊는다 — 중간이 빠진 오디오를 하나의 구간인 척 이어 붙이지 않는다.
                seg = Segment(id = segmentIdGenerator.incrementAndGet(), startIndex = cursor, rateHz = rate)
                segmentRef.set(seg)
                consumed = cursor
            }

            buf[(cursor and RingMask).toInt()] = sum / channels
            cursor++
        }

        writeCursor = cursor
        consumedCursor = consumed
    }

    override fun poll(): PcmChunk? {
        if (!capturing) return null
        val buf = ring ?: return null

        // 읽기 순서: seg → write index → seg 재확인. 그 사이 flush/오버런/startCapture 로
        // 구간이 바뀌었으면 방금 잰 writeSnapshot 이 어느 구간의 것인지 알 수 없다 — 이번
        // 틱은 버리고 새 구간에 커서만 맞춰 둔다. 다음 100ms 에 다시 시도되니 손해가 없다.
        val seg = segmentRef.get()
        val writeSnapshot = writeCursor
        val segAfter = segmentRef.get()
        if (segAfter !== seg) {
            pollSegmentId = segAfter.id
            readCursor = segAfter.startIndex
            return null
        }

        if (pollSegmentId != seg.id) {
            pollSegmentId = seg.id
            readCursor = seg.startIndex
        }

        // 정확히 100ms(= rate/10) 모였을 때만 내보낸다. 모자라면 다음 poll 까지 기다리고,
        // 남는 것은 다음 청크의 앞부분으로 남겨 둔다 — 한 청크가 두 구간·두 레이트를
        // 섞으면 sherpa 가 그 stream 에서 죽는다.
        val chunkSize = seg.rateHz / SamplesPerChunkDivisor
        if (chunkSize <= 0 || writeSnapshot - readCursor < chunkSize) return null

        val out = chunkBuffers.getOrPut(seg.rateHz) { FloatArray(chunkSize) }
        for (i in 0 until chunkSize) {
            out[i] = buf[((readCursor + i) and RingMask).toInt()]
        }
        readCursor += chunkSize
        consumedCursor = readCursor

        return PcmChunk(segment = seg.id, sampleRateHz = seg.rateHz, samples = out)
    }

    /**
     * 한 구간의 시작 지점과 레이트. [AtomicReference] 한 칸으로 건너가므로 시작 지점과
     * 레이트가 어긋난 짝으로 읽히지 않는다.
     */
    private data class Segment(val id: Int, val startIndex: Long, val rateHz: Int)

    private companion object {
        /** 링버퍼 크기(표본 수). 2 의 거듭제곱이라 나눗셈 대신 마스킹으로 인덱스를 접는다. */
        const val RingSize = 1 shl 19
        const val RingMask = (RingSize - 1).toLong()

        const val BytesPerSample = 2

        const val DefaultSampleRateHz = 44_100

        /** 청크 하나 = 100ms. */
        const val SamplesPerChunkDivisor = 10
    }
}
