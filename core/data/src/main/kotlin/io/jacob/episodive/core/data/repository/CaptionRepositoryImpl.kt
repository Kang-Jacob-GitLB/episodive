package io.jacob.episodive.core.data.repository

import dagger.Lazy
import io.jacob.episodive.core.caption.asset.CaptionModelManager
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
import io.jacob.episodive.core.model.caption.CaptionModelState
import io.jacob.episodive.core.model.caption.CaptionSession
import io.jacob.episodive.core.model.caption.LiveCaption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
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

    override fun translatedCues(
        episodeId: Long,
        sourceLanguageTag: String,
        cues: Flow<String>,
    ): Flow<LiveCaption?> {
        val translator = captionTranslatorFactory.create(sourceLanguageTag, deviceLanguageProvider.languageTag())
            ?: return cues.withIndex().map { (index, cue) -> cue.toLiveCaptionOrNull(episodeId, index.toLong(), translation = null) }

        return channelFlow { collectTranslatedCues(cues, translator, episodeId) }
    }

    /**
     * cue 마다 원문을 먼저 즉시 내보내고, 번역이 도착하면 같은 lineId 로 다시 내보낸다.
     * `collectLatest` 로 cue 가 바뀌면 진행 중이던 번역을 취소한다 — 번역기가 진짜 suspend
     * (모델 다운로드·추론)라 오래 걸릴 수 있는데, 그 사이 다음 cue 로 넘어갔으면 늦게 온
     * 번역은 애초에 시작하지 않는다.
     */
    private suspend fun ProducerScope<LiveCaption?>.collectTranslatedCues(
        cues: Flow<String>,
        translator: CaptionTranslator,
        episodeId: Long,
    ) {
        // 모델 다운로드가 걸릴 수 있어 STT/원문 방출을 막지 않도록 따로 띄운다. 준비 전에
        // translate() 를 부르면 CaptionTranslator 계약상 null 이 오므로 원문만 먼저 나간다.
        launch { translator.prepare() }
        try {
            cues.withIndex().collectLatest { (index, cue) ->
                val lineId = index.toLong()
                val original = cue.toLiveCaptionOrNull(episodeId, lineId, translation = null)
                send(original)
                if (original == null) return@collectLatest
                val translated = translator.translate(cue) ?: return@collectLatest
                send(original.copy(translation = translated))
            }
        } finally {
            translator.close()
        }
    }

    private fun String.toLiveCaptionOrNull(episodeId: Long, lineId: Long, translation: String?): LiveCaption? =
        takeIf { it.isNotEmpty() }?.let {
            LiveCaption(episodeId = episodeId, lineId = lineId, text = it, translation = translation, isFinal = true)
        }
}
