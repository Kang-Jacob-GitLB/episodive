package io.jacob.episodive.core.caption.engine

import io.jacob.episodive.core.caption.asset.InstalledCaptionModel

/** 설치된 모델로 [SpeechRecognizer] 를 올린다. sherpa-onnx 어댑터가 구현한다. */
interface SpeechRecognizerFactory {
    fun create(model: InstalledCaptionModel): SpeechRecognizer
}

/** 네이티브 인식기 하나. 단일 스레드에서만 쓴다(`@CaptionThread`). */
interface SpeechRecognizer : AutoCloseable {
    fun createStream(sampleRateHz: Int): RecognitionStream
}

/**
 * 구간(Segment) 하나에 대응하는 인식 스트림. `sampleRateHz` 는 생성 시 고정되고,
 * 이후 다른 레이트의 샘플을 [accept] 하면 구현체가 `require` 로 막는다
 * (sherpa-onnx 네이티브는 스트림 안에서 레이트가 바뀌면 프로세스를 죽인다).
 */
interface RecognitionStream : AutoCloseable {
    val sampleRateHz: Int

    /**
     * @param sampleRateHz [samples] 를 잰 실제 레이트. 생성 시 고정된 [RecognitionStream.sampleRateHz]
     *   와 다르면 구현체가 `require` 로 막는다. 레이트를 인자로 받지 않으면 이 검사를 할 수
     *   없고, 그러면 잘못된 레이트가 네이티브까지 내려가 프로세스가 `exit(-1)` 로 죽는다.
     *   [io.jacob.episodive.core.player.audio.PcmChunk] 가 청크마다 레이트를 함께 들고 오므로
     *   호출부는 그 값을 그대로 넘기면 된다.
     */
    fun accept(samples: FloatArray, sampleRateHz: Int)
    fun decodeAvailable()
    fun isEndpoint(): Boolean
    fun tokens(): List<String>

    /** 발화 경계에서 다음 발화를 위해 누적 토큰만 리셋한다. 리샘플러는 건드리지 않는다. */
    fun resetUtterance()
}
