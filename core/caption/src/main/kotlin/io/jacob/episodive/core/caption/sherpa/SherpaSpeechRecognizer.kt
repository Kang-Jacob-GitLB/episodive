package io.jacob.episodive.core.caption.sherpa

import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineStream
import io.jacob.episodive.core.caption.engine.RecognitionStream
import io.jacob.episodive.core.caption.engine.SpeechRecognizer

/**
 * sherpa-onnx `OnlineRecognizer` 하나를 감싼다. 네이티브 포인터를 들고 있는 객체라
 * [RecognizerCache][io.jacob.episodive.core.caption.engine.RecognizerCache] 가 정한
 * `@CaptionThread` 단일 스레드 밖에서 쓰면 안 된다 — 두 스레드가 동시에 스트림을 건드리면
 * 네이티브 쪽 레이스로 이어진다.
 */
internal class SherpaSpeechRecognizer(private val recognizer: OnlineRecognizer) : SpeechRecognizer {
    override fun createStream(sampleRateHz: Int): RecognitionStream =
        SherpaRecognitionStream(recognizer, recognizer.createStream(), sampleRateHz)

    override fun close() {
        recognizer.release()
    }
}

/**
 * 구간(Segment) 하나에 대응하는 sherpa-onnx `OnlineStream`.
 *
 * `decode`/`isReady`/`isEndpoint`/`getResult`/`reset` 은 스트림이 아니라 [recognizer] 가
 * 들고 있는 메서드다(sherpa-onnx 1.13.8 API) — `OnlineStream` 자체는 `acceptWaveform` 과
 * `inputFinished`/`setOption` 정도만 가진다.
 */
internal class SherpaRecognitionStream(
    private val recognizer: OnlineRecognizer,
    private val stream: OnlineStream,
    override val sampleRateHz: Int,
) : RecognitionStream {
    override fun accept(samples: FloatArray, sampleRateHz: Int) {
        // 여기서 막지 않으면 네이티브가 exit(-1) 로 프로세스를 통째로 죽인다 — Kotlin 예외가
        // 훨씬 싸다. (`RecognitionStream.accept` 문서 참고.)
        require(sampleRateHz == this.sampleRateHz) {
            "스트림은 ${this.sampleRateHz}Hz 로 고정돼 있는데 ${sampleRateHz}Hz 샘플이 들어왔다"
        }
        stream.acceptWaveform(samples, sampleRateHz)
    }

    override fun decodeAvailable() {
        while (recognizer.isReady(stream)) {
            recognizer.decode(stream)
        }
    }

    override fun isEndpoint(): Boolean = recognizer.isEndpoint(stream)

    override fun tokens(): List<String> = recognizer.getResult(stream).tokens.toList()

    override fun resetUtterance() {
        // sherpa 의 reset(stream) 은 발화 단위 디코더 상태만 지운다. 리샘플 버퍼는 스트림 안에
        // 그대로 남아 다음 발화로 이어진다 — 그래서 구간이 바뀌지 않은 endpoint 에서는 스트림을
        // 새로 만들지 않고 이 메서드만 부른다.
        recognizer.reset(stream)
    }

    override fun close() {
        stream.release()
    }
}
