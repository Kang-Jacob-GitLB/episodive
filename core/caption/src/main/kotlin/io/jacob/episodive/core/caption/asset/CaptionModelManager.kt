package io.jacob.episodive.core.caption.asset

import io.jacob.episodive.core.model.caption.CaptionDownloadFailure
import io.jacob.episodive.core.model.caption.CaptionLanguage
import io.jacob.episodive.core.model.caption.CaptionModelState
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * 언어별 STT 모델의 설치·다운로드 상태를 관리한다(앱 수명 [scope] 에서 돈다).
 *
 * 다운로드는 [start] 로 시작하고 [cancel] 로 취소한다. 두 번째 [start] 는 이미 받는 중이면
 * 아무 일도 하지 않는다(버튼 연타로 같은 파일을 두 번 받지 않게) — `jobs` 맵에 대한 갱신을
 * [ConcurrentHashMap.compute] 로 원자적으로 처리해, 동기 호출인 [start] 자체가 레이스 없이
 * "이미 진행 중인가"를 판정한다(코루틴이 실제로 실행되길 기다릴 필요가 없다).
 */
class CaptionModelManager(
    private val store: CaptionModelStore,
    private val downloader: CaptionModelDownloader,
    private val scope: CoroutineScope,
    private val specs: List<CaptionModelSpec> = CaptionModelRegistry.specs,
) {

    private val states: Map<CaptionLanguage, MutableStateFlow<CaptionModelState>> =
        specs.associate { spec -> spec.language to MutableStateFlow(initialState(spec)) }

    private val jobs = ConcurrentHashMap<CaptionLanguage, Job>()

    private val mutableDownloadFailures = MutableSharedFlow<CaptionDownloadFailure>(extraBufferCapacity = 1)
    val downloadFailures: SharedFlow<CaptionDownloadFailure> = mutableDownloadFailures.asSharedFlow()

    init {
        // 레지스트리에서 빠진 언어가 받아 둔 모델(구버전 잔해 등)을 정리.
        store.pruneUnregistered()
    }

    fun state(language: CaptionLanguage): StateFlow<CaptionModelState> = mutableState(language)

    fun installed(language: CaptionLanguage): InstalledCaptionModel? = store.installed(language)

    fun start(language: CaptionLanguage) {
        val spec = specFor(language) ?: return
        val stateFlow = mutableState(language)
        if (stateFlow.value is CaptionModelState.Installed) return

        jobs.compute(language) { _, existing ->
            if (existing?.isActive == true) return@compute existing
            scope.launch { runDownload(language, spec, stateFlow) }
        }
    }

    fun cancel(language: CaptionLanguage) {
        // 진행 중이던 job 이 있을 때만 취소하고 상태를 되돌린다. job.cancel() 은 협조적 취소라
        // 블로킹 IO 도중이면 코루틴이 알아채기까지 시간이 걸린다 — 그동안 화면이 여전히
        // Downloading 으로 보이지 않도록, 코루틴이 실제로 멈추길 기다리지 않고 여기서 바로
        // 되돌린다. runDownload 의 finally 는 jobs.remove(language, thisJob) 로 자기 항목만
        // 지우므로, 이 취소 직후 곧바로 start() 해 새 job 이 들어와도 그 job 의 자리를
        // 건드리지 않는다.
        val job = jobs.remove(language) ?: return
        job.cancel()
        val spec = specFor(language) ?: return
        mutableState(language).value = CaptionModelState.NotInstalled(spec.totalSizeBytes)
    }

    fun invalidate(language: CaptionLanguage) {
        jobs.remove(language)?.cancel()
        val spec = specFor(language) ?: return
        store.invalidate(language)
        mutableState(language).value = initialState(spec)
    }

    /**
     * [io.jacob.episodive.core.caption.engine.LiveCaptionEngine] 세션이 인식기를 올리지 못해
     * `Unloadable` 을 낸 경우 CaptionRepositoryImpl 이 호출한다. [invalidate] 로 설치본을 지워
     * `modelState` 가 `NotInstalled` 로 되돌아가게 하고(다음에 다시 받을 수 있게), 그 사실을
     * [downloadFailures] 로 알린다 — 다운로드 실패가 아니라 로드 실패지만 사용자에게는 같은
     * 스낵바 경로로 보이면 된다.
     */
    fun reportUnloadable(language: CaptionLanguage) {
        invalidate(language)
        mutableDownloadFailures.tryEmit(CaptionDownloadFailure(language, CaptionDownloadFailure.Reason.UNLOADABLE))
    }

    private fun mutableState(language: CaptionLanguage): MutableStateFlow<CaptionModelState> =
        states[language] ?: error("caption model spec not registered for $language")

    private suspend fun runDownload(
        language: CaptionLanguage,
        spec: CaptionModelSpec,
        stateFlow: MutableStateFlow<CaptionModelState>,
    ) {
        val thisJob = currentCoroutineContext()[Job]
        var cancelled = false
        try {
            val total = spec.totalSizeBytes
            var downloaded = store.downloadedBytes(spec)
            var lastPercent = percentOf(downloaded, total)
            stateFlow.value = CaptionModelState.Downloading(downloaded, total)

            val reason = downloader.download(spec) { delta ->
                // downloader 가 "이어받기를 포기하고 처음부터 다시 받는다" 를 알릴 때 음수
                // 델타를 보낸다(CaptionModelDownloader 문서 참고) — 0 밑으로는 내려가지 않게 막는다.
                downloaded = (downloaded + delta).coerceAtLeast(0L)
                val percent = percentOf(downloaded, total)
                // 1% 단위로만 방출 — 100ms STT 청크 수준으로 세밀한 바이트 콜백을 그대로
                // StateFlow 로 흘리면 재구성이 과하게 자주 돈다.
                if (percent != lastPercent) {
                    lastPercent = percent
                    stateFlow.value = CaptionModelState.Downloading(downloaded, total)
                }
            }

            stateFlow.value = if (reason == null) {
                CaptionModelState.Installed
            } else {
                mutableDownloadFailures.emit(CaptionDownloadFailure(language, reason))
                CaptionModelState.NotInstalled(total)
            }
        } catch (e: CancellationException) {
            cancelled = true
            throw e
        } finally {
            // 자기 항목만 지운다(jobs.remove(key, value) 는 현재 매핑이 thisJob 일 때만 지우고
            // true 를 돌려준다) — cancel() 이 이미 이 job 을 지우고 새 job 을 등록했다면(빠른
            // 취소 후 재시작) 그 새 job 의 자리를 여기서 건드리면 안 된다.
            val stillCurrent = thisJob == null || jobs.remove(language, thisJob)
            if (cancelled && stillCurrent) {
                // cancel() 이 이미 상태를 되돌렸다면 stillCurrent 가 false 라 여기로 오지 않는다.
                // 여기 남는 경우는 manager.cancel() 이 아닌 다른 경로(스코프 자체 취소 등)로
                // 코루틴이 취소된 경우다.
                stateFlow.value = CaptionModelState.NotInstalled(spec.totalSizeBytes)
            }
        }
    }

    private fun specFor(language: CaptionLanguage): CaptionModelSpec? = specs.find { it.language == language }

    private fun initialState(spec: CaptionModelSpec): CaptionModelState =
        if (store.isInstalled(spec)) {
            CaptionModelState.Installed
        } else {
            CaptionModelState.NotInstalled(spec.totalSizeBytes)
        }

    private fun percentOf(downloaded: Long, total: Long): Int =
        if (total <= 0L) 100 else ((downloaded * 100) / total).toInt().coerceIn(0, 100)
}
