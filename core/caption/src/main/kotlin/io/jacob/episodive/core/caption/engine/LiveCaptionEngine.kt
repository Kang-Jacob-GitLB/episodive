package io.jacob.episodive.core.caption.engine

import io.jacob.episodive.core.caption.asset.InstalledCaptionModel
import io.jacob.episodive.core.caption.text.CaptionLineTracker
import io.jacob.episodive.core.model.caption.CaptionLine
import io.jacob.episodive.core.model.caption.CaptionSession
import io.jacob.episodive.core.model.caption.LiveCaption
import io.jacob.episodive.core.player.audio.SpeechPcmSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 에피소드 하나에 대한 라이브 자막 세션. `session()` 을 **수집하는 동안만** 인식이 돈다 —
 * 수집이 끊기면(시트가 닫히거나 재생이 멎으면) [SpeechPcmSource.stopCapture] 와 인식기 반납까지
 * 정리된다(아래 `finally`).
 *
 * 루프 전체가 [CaptionThread] 단일 스레드에서 돈다: sherpa-onnx 의 네이티브 스트림이 단일
 * 스레드 전제라서다([SpeechRecognizer] 문서 참고). 번역은 그 안에서 **별도 코루틴으로 띄워**
 * STT 디코드를 막지 않는다 — `translate()` 가 진짜 suspend 함수라 기다리는 동안 같은 스레드의
 * 다른 코루틴(다음 디코드 틱)이 끼어들 수 있고, 그래서 굳이 다른 스레드로 옮기지 않아도
 * "인식 루프를 막지 않는다" 는 요구를 만족한다.
 */
@Singleton
class LiveCaptionEngine @Inject constructor(
    private val pcm: SpeechPcmSource,
    private val recognizers: RecognizerCache,
    private val translators: CaptionTranslatorFactory,
    private val deviceLanguage: DeviceLanguageProvider,
    @CaptionThread private val dispatcher: CoroutineDispatcher,
) {
    fun session(episodeId: Long, model: InstalledCaptionModel): Flow<CaptionSession> = channelFlow {
        pcm.startCapture()
        // startCapture() 직후부터는 어떤 경로로 끝나든(send 취소, translators.create 예외,
        // Unloadable/Failed) 반드시 stopCapture() 까지 간다 — 이 try 를 recognizers.acquire()
        // 나 translators.create() 를 감싸지 않고 그 아래에서만 걸면, 그 둘이 던지는 예외가
        // pcm 을 캡처 중인 채로 그대로 새 버린다. 인식기는 "받았을 때만" release() 한다.
        var recognizer: SpeechRecognizer? = null
        try {
            send(CaptionSession.Loading)

            when (val acquired = recognizers.acquire(model)) {
                is RecognizerAcquireResult.Unloadable -> {
                    send(CaptionSession.Unloadable)
                    return@channelFlow
                }
                RecognizerAcquireResult.Failed -> {
                    // 일시 실패 — 모델을 지우면 안 되니 Unloadable 을 보내지 않는다(그러면 호출부가
                    // 모델을 지운다). 세션만 조용히 끝내 Loading 에 멈춰 있지 않게 한다.
                    return@channelFlow
                }
                is RecognizerAcquireResult.Acquired -> recognizer = acquired.recognizer
            }
            // 위 when 의 Unloadable/Failed 갈래는 이미 return@channelFlow 로 빠져나갔으므로 여기
            // 도달했다는 것 자체가 Acquired 였다는 뜻이다 — recognizer 는 항상 non-null 이다.
            val activeRecognizer = requireNotNull(recognizer)

            // 원문 언어와 기기 언어가 같거나 ML Kit 이 지원하지 않으면 null — 그 뒤로는 번역을
            // 아예 시도하지 않는다.
            val translator = translators.create(
                sourceTag = model.language.value,
                targetTag = deviceLanguage.languageTag(),
            )
            try {
                // 모델 다운로드가 걸릴 수 있어 STT 준비를 막지 않도록 따로 띄운다.
                translator?.let { t -> launch { t.prepare() } }

                val tracker = CaptionLineTracker(model.language, model.maxLineChars)
                val presenter = CaptionPresenter(episodeId, isTranslating = translator != null)
                var stream: RecognitionStream? = null
                var currentSegment: Int? = null
                var lastSent: LiveCaption? = null

                suspend fun publish(caption: LiveCaption?) {
                    if (caption == lastSent) return
                    lastSent = caption
                    send(CaptionSession.Running(caption))
                }

                suspend fun translateAndPublish(line: CaptionLine) {
                    val t = translator ?: return
                    val translated = t.translate(line.text) ?: return
                    publish(presenter.onTranslation(line.id, translated, line.utteranceId))
                }

                try {
                    while (isActive) {
                        val chunk = pcm.poll()
                        if (chunk == null) {
                            delay(PollDelayMillis)
                            continue
                        }

                        // 구간이 바뀌었다(시크·EOS·오버런) — 이전 스트림은 지난 소리를 담고 있으니
                        // 버리고 새로 연다. `recognizer.reset` 을 쓰지 않는 것은 리샘플러 상태까지
                        // 이어받기 때문이다(CLAUDE.md "인식·표시" 절).
                        if (chunk.segment != currentSegment) {
                            stream?.close()
                            stream = activeRecognizer.createStream(chunk.sampleRateHz)
                            currentSegment = chunk.segment
                            tracker.reset()
                            if (chunk.isContinuation) {
                                // 오버런 — 오디오만 끊겼고 재생 위치는 이어진다. 화면을 비우면 커버
                                // 전체가 페이드아웃했다 다시 차므로, 흘러가던 줄만 확정으로 남긴다.
                                presenter.finalizePartial()?.let { line ->
                                    publish(presenter.current())
                                    launch { translateAndPublish(line) }
                                }
                            } else {
                                presenter.clear()
                                publish(null)
                            }
                        }
                        val activeStream = stream ?: continue

                        activeStream.accept(chunk.samples, chunk.sampleRateHz)
                        activeStream.decodeAvailable()

                        val tokens = activeStream.tokens()
                        val update = tracker.onTokens(tokens)
                        update.finalized?.let { line ->
                            presenter.onFinalized(line)
                            launch { translateAndPublish(line) }
                        }
                        publish(presenter.onPartial(update.partial))

                        if (activeStream.isEndpoint()) {
                            val finalLine = tracker.onEndpoint(tokens)
                            if (finalLine != null) {
                                publish(presenter.onFinalized(finalLine))
                                launch { translateAndPublish(finalLine) }
                            }
                            activeStream.resetUtterance()
                        }
                    }
                } finally {
                    // 수집이 취소되면(시트 닫힘 등) 여기로 온다. 일반 반환(while(isActive) 를
                    // 빠져나오는 경로)은 사실상 없다 — 취소만이 루프를 끝낸다.
                    stream?.close()
                }
            } finally {
                translator?.close()
            }
        } finally {
            pcm.stopCapture()
            recognizer?.let { recognizers.release(model.language) }
        }
    }.flowOn(dispatcher)

    private companion object {
        /** [SpeechPcmSource.poll] 이 null 이면(아직 100ms 가 안 모였으면) 이만큼 쉬고 다시 본다. */
        const val PollDelayMillis = 20L
    }
}
