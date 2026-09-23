package io.jacob.episodive.core.caption.engine

import io.jacob.episodive.core.caption.asset.InstalledCaptionModel
import io.jacob.episodive.core.model.caption.CaptionLanguage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** [RecognizerCache.acquire] 결과. */
sealed interface RecognizerAcquireResult {
    data class Acquired(val recognizer: SpeechRecognizer) : RecognizerAcquireResult

    /** 연속 2회 미완이라 이번에는 로드를 시도조차 하지 않았거나, 이번 시도가 연속 2회째로 실패했다.
     * 이 경우에만 호출부가 모델 자체를 지운다([io.jacob.episodive.core.caption.asset.CaptionModelManager.reportUnloadable]). */
    data object Unloadable : RecognizerAcquireResult

    /** 이번 시도가 코틀린 예외로 실패했지만 아직 연속 2회가 아니다. 모델은 그대로 두는 일시
     * 실패다 — 이번 세션만 조용히 끝내고, 다음 세션에서 다시 시도할 수 있게 한다. */
    data object Failed : RecognizerAcquireResult
}

/**
 * sherpa-onnx `OnlineRecognizer` 는 무겁다(모델을 통째로 메모리에 올린다) — 세션(에피소드)이
 * 바뀔 때마다 새로 만들고 버리면 짧게 오가는 사용자에게 매번 로드 지연을 물린다. 그래서
 * 세션이 끝나도 [keepAliveMillis] 동안은 살려 둔다.
 *
 * 네이티브 로더가 **프로세스를 통째로 죽이며** 실패할 수 있다(예: onnx 파일이 손상됐거나
 * 기기가 감당 못 하는 경우). 그 경우 다음 실행에서 `.loading` 파일의 시도 횟수만으로 그 사실을
 * 알아챌 수 있다 — 성공하면 지우고, 실패(=파일이 남아 있음)가 두 번 연속이면 더는 시도하지
 * 않는다.
 */
@Singleton
class RecognizerCache internal constructor(
    private val factory: SpeechRecognizerFactory,
    dispatcher: CoroutineDispatcher,
    private val keepAliveMillis: Long = DefaultKeepAliveMillis,
) {
    @Inject
    constructor(
        factory: SpeechRecognizerFactory,
        @CaptionThread dispatcher: CoroutineDispatcher,
    ) : this(factory, dispatcher, DefaultKeepAliveMillis)

    /** keep-alive 지연 종료를 스케줄링하는 스코프. [dispatcher] 가 단일 스레드라 [entry] 접근에
     * 별도 락이 필요 없다 — acquire/release 도 이 스레드에서만 불린다는 전제다. */
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private var entry: Entry? = null

    /**
     * [model] 언어의 인식기를 반환한다. 캐시에 같은 언어가 있으면 그대로 재사용(keep-alive
     * 타이머가 걸려 있었다면 취소). 다른 언어가 있으면 **즉시** 닫고 새로 올린다 — 동시에 두
     * 언어를 들고 있을 이유가 없다.
     */
    fun acquire(model: InstalledCaptionModel): RecognizerAcquireResult {
        val current = entry
        if (current != null && current.language == model.language) {
            current.releaseJob?.cancel()
            current.releaseJob = null
            return RecognizerAcquireResult.Acquired(current.recognizer)
        }
        current?.let {
            it.releaseJob?.cancel()
            it.recognizer.close()
        }
        entry = null

        val modelDir = File(model.encoderPath).parentFile
            ?: return RecognizerAcquireResult.Unloadable

        val attempts = readLoadAttempts(modelDir)
        if (attempts >= MaxLoadAttempts) return RecognizerAcquireResult.Unloadable

        val nextAttempts = attempts + 1
        writeLoadAttempts(modelDir, nextAttempts)
        val recognizer = try {
            factory.create(model)
        } catch (t: Throwable) {
            // `.loading` 은 지우지 않는다 — 다음 시도도 실패하면 그때 위 카운트가 막는다.
            // 코틀린 예외로 실패한 첫 시도를 곧바로 Unloadable 로 단정하면 호출부(CaptionModelManager)
            // 가 그 자리에서 모델을 지운다 — 메모리 부족처럼 일시적일 수 있는 실패라 연속 2회가
            // 될 때까지는 모델을 지우지 않는다(RecognizerAcquireResult.Failed 문서 참고).
            return if (nextAttempts >= MaxLoadAttempts) {
                RecognizerAcquireResult.Unloadable
            } else {
                RecognizerAcquireResult.Failed
            }
        }
        loadingFile(modelDir).delete()
        entry = Entry(model.language, recognizer, null)
        return RecognizerAcquireResult.Acquired(recognizer)
    }

    /**
     * 세션이 끝났다. 바로 닫지 않고 [keepAliveMillis] 뒤로 미룬다 — 그 사이 같은 언어로
     * [acquire] 가 다시 오면 그 호출이 이 타이머를 취소하고 재사용한다.
     */
    fun release(language: CaptionLanguage) {
        val current = entry ?: return
        if (current.language != language) return
        current.releaseJob?.cancel()
        current.releaseJob = scope.launch {
            delay(keepAliveMillis)
            if (entry === current) {
                current.recognizer.close()
                entry = null
            }
        }
    }

    private fun loadingFile(modelDir: File): File = File(modelDir, LoadingFileName)

    private fun readLoadAttempts(modelDir: File): Int =
        loadingFile(modelDir).takeIf { it.exists() }
            ?.runCatching { readText().trim().toInt() }
            ?.getOrNull()
            ?: 0

    private fun writeLoadAttempts(modelDir: File, attempts: Int) {
        // 이 마커는 "네이티브 크래시로 전체 프로세스가 죽는" 드문 경우를 다음 실행에서 알아채기
        // 위한 최선 노력용 장치다. 디스크 권한·용량 문제로 쓰기 자체가 실패해도 그건 로드 시도를
        // 막을 이유가 아니다 — 조용히 넘어가고 실제 로드(factory.create)는 그대로 시도한다.
        runCatching {
            modelDir.mkdirs()
            loadingFile(modelDir).writeText(attempts.toString())
        }
    }

    private class Entry(
        val language: CaptionLanguage,
        val recognizer: SpeechRecognizer,
        var releaseJob: Job?,
    )

    private companion object {
        const val LoadingFileName = ".loading"
        const val MaxLoadAttempts = 2
        const val DefaultKeepAliveMillis = 30_000L
    }
}
