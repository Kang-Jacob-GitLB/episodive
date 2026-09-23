package io.jacob.episodive.core.data.repository

import dagger.Lazy
import io.jacob.episodive.core.caption.asset.CaptionModelManager
import io.jacob.episodive.core.caption.engine.CaptionPresenter
import io.jacob.episodive.core.caption.engine.CaptionTranslator
import io.jacob.episodive.core.caption.engine.CaptionTranslatorFactory
import io.jacob.episodive.core.caption.engine.DeviceLanguageProvider
import io.jacob.episodive.core.caption.engine.LiveCaptionEngine
import io.jacob.episodive.core.common.ApplicationScope
import io.jacob.episodive.core.common.Dispatcher
import io.jacob.episodive.core.common.EpisodiveDispatchers
import io.jacob.episodive.core.domain.repository.CaptionRepository
import io.jacob.episodive.core.model.caption.CaptionDownloadFailure
import io.jacob.episodive.core.model.caption.CaptionLanguage
import io.jacob.episodive.core.model.caption.CaptionLine
import io.jacob.episodive.core.model.caption.CaptionModelState
import io.jacob.episodive.core.model.caption.CaptionSession
import io.jacob.episodive.core.model.caption.LiveCaption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [CaptionModelManager] 는 생성자에서 [CaptionModelManager] 문서에 적힌 동기 파일 IO
 * (`pruneUnregistered`) 를 한다. 그래서 여기서는 그 인스턴스를 직접 받지 않고
 * `dagger.Lazy<CaptionModelManager>` 로 받아, [manager] 를 거칠 때만 [ioDispatcher] 위에서
 * `.get()` 을 부른다 — 최초 접근이 언제 어느 스레드에서 일어나든(호출부가 메인이어도) 그
 * IO 는 항상 IO 디스패처에서 일어난다. `dagger.Lazy` 는 `DoubleCheck` 로 스레드 세이프하게
 * 메모이즈되므로 이후 접근은 사실상 필드 읽기 수준이다.
 */
class CaptionRepositoryImpl @Inject constructor(
    private val captionModelManager: Lazy<CaptionModelManager>,
    private val liveCaptionEngine: LiveCaptionEngine,
    private val captionTranslatorFactory: CaptionTranslatorFactory,
    private val deviceLanguageProvider: DeviceLanguageProvider,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
    @param:Dispatcher(EpisodiveDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : CaptionRepository {

    private suspend fun manager(): CaptionModelManager = withContext(ioDispatcher) { captionModelManager.get() }

    override fun modelState(language: CaptionLanguage): Flow<CaptionModelState> = flow {
        emitAll(manager().state(language))
    }

    override val downloadFailures: Flow<CaptionDownloadFailure> = flow {
        emitAll(manager().downloadFailures)
    }

    override fun startDownload(language: CaptionLanguage) {
        // start/cancel 자체는 논블로킹이지만 manager() 의 최초 획득은 IO 를 태우므로, 호출부
        // 스레드(대개 메인)를 막지 않도록 앱 수명 스코프에 태운다.
        applicationScope.launch(ioDispatcher) { manager().start(language) }
    }

    override fun cancelDownload(language: CaptionLanguage) {
        applicationScope.launch(ioDispatcher) { manager().cancel(language) }
    }

    override fun liveCaptions(episodeId: Long, language: CaptionLanguage): Flow<CaptionSession> = flow {
        // installed() 는 store.installed() 로 파일 stat 을 한다. manager() 는 `.get()` 만
        // ioDispatcher 위에서 부르고 리턴하므로, 뒤이은 installed() 호출은 이 flow 의 수집
        // 컨텍스트(대개 메인)에서 돈다 — withContext 로 감싸 IO 를 IO 디스패처에 묶는다.
        val model = withContext(ioDispatcher) { manager().installed(language) } ?: return@flow
        emitAll(
            liveCaptionEngine.session(episodeId, model).onEach { session ->
                if (session is CaptionSession.Unloadable) {
                    // 인식기를 올리지 못했다 — 설치본을 지우고(NotInstalled 로 되돌리고) 실패를
                    // 알린다. Manager 문서 참고. invalidate() 가 파일을 지우므로 이것도 IO.
                    withContext(ioDispatcher) { manager().reportUnloadable(language) }
                }
            }
        )
    }

    /**
     * [CaptionPresenter] 를 그대로 재사용한다 — VTT cue 도 "최근 N개 줄 + 늦게 온 번역은
     * 버린다" 규칙이 라이브 인식과 같다. cue 는 늘 확정 줄([CaptionLine.isFinal] =
     * true)로만 들어온다는 점만 다르다.
     */
    override fun translatedCues(
        episodeId: Long,
        sourceLanguageTag: String,
        cues: Flow<String>,
        seeks: Flow<Unit>,
    ): Flow<LiveCaption?> {
        val translator = captionTranslatorFactory.create(sourceLanguageTag, deviceLanguageProvider.languageTag())
        // 늦게 온 번역을 presenter 가 거절해도 현재 값을 한 번 더 돌려준다 — 같은 값은 흘리지 않는다
        // (엔진의 `lastSent` 와 같은 규칙).
        return channelFlow { collectTranslatedCues(cues, seeks, translator, episodeId) }
            .distinctUntilChanged()
    }

    /** [cues] 와 [seeks] 를 하나의 흐름으로 합치기 위한, 이 함수 안에서만 쓰는 사건 구분. */
    private sealed interface CueEvent {
        data class Cue(val text: String) : CueEvent
        data object Seek : CueEvent
    }

    /**
     * cue 마다 원문을 먼저 즉시 내보내고, 번역이 도착하면 그 줄의 번역을 덧붙여 다시 내보낸다.
     * 번역은 cue 마다 따로 띄운다 — 화면이 지난 줄들을 쌓아 두므로, 다음 cue(또는 cue 사이의
     * 빈 cue)가 왔다고 앞 줄의 번역을 취소하면 원문은 남았는데 번역만 영영 빠진다. 뒤늦게 온
     * 번역·시크 전 줄의 번역은 [CaptionPresenter.onTranslation] 의 하한이 거른다(STT 와 같은 구조).
     * 빈 cue(cue 사이 공백)는 건너뛴다 — 여기서 비우면 cue 가 바뀔 때마다 커버 전체가 비었다
     * 다시 찬다.
     *
     * [seeks] 와 같은 흐름으로 묶어 한 곳에서 수집하고, presenter 는 thread-safe 하지 않으므로
     * 번역 코루틴까지 포함해 [Mutex] 안에서만 건드린다. 시크가 오면 presenter 를 비우고 그 결과(=
     * null)를 곧바로 내보낸다. lineId 는 cue 사건에서만 늘려, 시크를 지나도 줄 번호가 계속
     * 증가하게 한다(번역 하한이 이 번호를 기준으로 지난 구간을 가른다).
     */
    private suspend fun ProducerScope<LiveCaption?>.collectTranslatedCues(
        cues: Flow<String>,
        seeks: Flow<Unit>,
        translator: CaptionTranslator?,
        episodeId: Long,
    ) {
        // 모델 다운로드가 걸릴 수 있어 STT/원문 방출을 막지 않도록 따로 띄운다. 준비 전에
        // translate() 를 부르면 CaptionTranslator 계약상 null 이 오므로 원문만 먼저 나간다.
        translator?.let { t -> launch { t.prepare() } }
        val presenter = CaptionPresenter(episodeId, isTranslating = translator != null)
        var nextLineId = 0L
        val presenterLock = Mutex()
        val events = merge(cues.map { CueEvent.Cue(it) }, seeks.map { CueEvent.Seek })
        try {
            // 번역 코루틴까지 끝난 뒤에 translator 를 닫도록 coroutineScope 로 묶는다 — cue 흐름이
            // 먼저 끝나도 진행 중인 translate() 가 닫힌 번역기를 부르지 않게.
            coroutineScope {
                events.collect { event ->
                    when (event) {
                        CueEvent.Seek -> presenterLock.withLock {
                            presenter.clear()
                            send(presenter.current())
                        }

                        is CueEvent.Cue -> {
                            if (event.text.isEmpty()) return@collect
                            val lineId = nextLineId++
                            presenterLock.withLock {
                                send(presenter.onFinalized(CaptionLine(id = lineId, text = event.text, isFinal = true)))
                            }
                            val t = translator ?: return@collect
                            launch {
                                val translated = t.translate(event.text) ?: return@launch
                                presenterLock.withLock { send(presenter.onTranslation(lineId, translated)) }
                            }
                        }
                    }
                }
            }
        } finally {
            translator?.close()
        }
    }
}
