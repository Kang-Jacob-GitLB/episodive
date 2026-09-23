package io.jacob.episodive.core.player.audio

/**
 * 라이브 자막(STT) 이 소비할 PCM 소스. `SpeechAudioTap` 이 Main 플레이어의 오디오 프로세서
 * 체인에 붙어 이 인터페이스를 구현한다.
 *
 * `startCapture()`/`stopCapture()` 로 캡처 여부를 켜고 끈다 — 자막이 꺼져 있으면
 * `poll()` 을 부르지 않는 것만으로 비용이 0 이어야 한다.
 */
interface SpeechPcmSource {
    fun startCapture()
    fun stopCapture()

    /** 한 구간 안에서 100ms 분량이 모였을 때만 청크를 반환한다. 그 전엔 null. */
    fun poll(): PcmChunk?
}

/**
 * [SpeechPcmSource.poll] 이 반환하는 PCM 청크. [samples] 는 레이트별로 재사용되는 배열이라
 * **다음 poll 전까지만 유효**하다 — 넘겨받은 쪽에서 들고 있지 말고 그 자리에서 소비한다.
 *
 * @param segment 구간 식별자. 시크·EOS·오버런 등으로 구간이 바뀌면 증가한다.
 * @param sampleRateHz [samples] 의 샘플레이트.
 * @param isContinuation 이 구간이 **오버런**(리더가 못 따라와 링이 넘침)으로 끊긴 것이면 true.
 * 오디오는 끊겼어도 재생 위치는 이어지므로, 화면의 자막을 비울 이유는 없다. 시크·EOS·리셋
 * (flush)이나 캡처 시작으로 열린 구간은 false.
 */
class PcmChunk(
    val segment: Int,
    val sampleRateHz: Int,
    val samples: FloatArray,
    val isContinuation: Boolean = false,
)
