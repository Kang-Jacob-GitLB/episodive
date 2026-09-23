package io.jacob.episodive.core.caption.engine

import io.jacob.episodive.core.caption.asset.InstalledCaptionModel
import io.jacob.episodive.core.model.caption.CaptionLanguage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * `.loading` 파일과 keep-alive 지연 모두 시간을 다루므로, [PlaybackSpectrumMonitorGateTest]
 * (`:core:player`) 와 같은 방식으로 [TestCoroutineScheduler] 를 직접 쥐고 돌린다 — `runTest` 의
 * 자동 진행에 맡기면 keep-alive 타이머가 몇 초짜리인지와 무관하게 테스트가 알아서 끝까지
 * 밀어버려, "아직 안 닫혔다" 를 검증할 시점을 잡을 수 없다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecognizerCacheTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)

    private fun model(language: CaptionLanguage, dirName: String = language.value): InstalledCaptionModel {
        val dir = tempFolder.newFolder(dirName)
        return InstalledCaptionModel(
            language = language,
            encoderPath = File(dir, "encoder.onnx").path,
            decoderPath = File(dir, "decoder.onnx").path,
            joinerPath = File(dir, "joiner.onnx").path,
            tokensPath = File(dir, "tokens.txt").path,
            maxLineChars = 80,
        )
    }

    private fun cache(factory: FakeSpeechRecognizerFactory, keepAliveMillis: Long = 30_000L): RecognizerCache =
        RecognizerCache(factory, dispatcher, keepAliveMillis)

    @Test
    fun `acquire creates a recognizer and release schedules a close after the keep-alive window`() {
        val factory = FakeSpeechRecognizerFactory()
        val cache = cache(factory)
        val model = model(CaptionLanguage.ENGLISH)

        val result = cache.acquire(model) as RecognizerAcquireResult.Acquired
        cache.release(CaptionLanguage.ENGLISH)

        scheduler.advanceTimeBy(29_999)
        scheduler.runCurrent()
        assertTrue("유지 시간이 아직 안 지났으면 닫으면 안 된다", !result.recognizer.let { it as FakeSpeechRecognizer }.closed)

        scheduler.advanceTimeBy(2)
        scheduler.runCurrent()
        assertTrue("유지 시간이 지나면 닫혀야 한다", (result.recognizer as FakeSpeechRecognizer).closed)
    }

    @Test
    fun `re-acquiring the same language within the keep-alive window reuses the instance and cancels the close`() {
        val factory = FakeSpeechRecognizerFactory()
        val cache = cache(factory)
        val model = model(CaptionLanguage.ENGLISH)

        val first = (cache.acquire(model) as RecognizerAcquireResult.Acquired).recognizer
        cache.release(CaptionLanguage.ENGLISH)
        scheduler.advanceTimeBy(10_000)
        scheduler.runCurrent()

        val second = (cache.acquire(model) as RecognizerAcquireResult.Acquired).recognizer
        scheduler.advanceTimeBy(30_000)
        scheduler.runCurrent()

        assertEquals("같은 인스턴스를 재사용해야 한다", first, second)
        assertTrue("재사용됐으니 예약된 close 는 취소돼 있어야 한다", !(first as FakeSpeechRecognizer).closed)
        assertEquals(1, factory.createCount)
    }

    @Test
    fun `acquiring a different language closes the previous one immediately`() {
        val factory = FakeSpeechRecognizerFactory()
        val cache = cache(factory)

        val english = (cache.acquire(model(CaptionLanguage.ENGLISH)) as RecognizerAcquireResult.Acquired).recognizer
        cache.acquire(model(CaptionLanguage.KOREAN, dirName = "ko"))

        assertTrue("keep-alive 를 기다리지 않고 즉시 닫아야 한다", (english as FakeSpeechRecognizer).closed)
    }

    @Test
    fun `a single failure is transient, but two consecutive incomplete loads make the model unloadable`() {
        val factory = FakeSpeechRecognizerFactory(failNextCreate = true)
        val cache = cache(factory)
        val model = model(CaptionLanguage.ENGLISH)

        // 첫 실패는 아직 연속 2회가 아니다 — 모델을 지우면 안 되는 일시 실패(Failed)로 끝난다.
        val first = cache.acquire(model)
        assertTrue("첫 실패는 Unloadable 이 아니라 일시 실패여야 한다", first is RecognizerAcquireResult.Failed)

        factory.failNextCreate = true
        val second = cache.acquire(model)
        assertTrue("연속 2회째 실패에서만 Unloadable 이 된다", second is RecognizerAcquireResult.Unloadable)

        // 세 번째부터는 시도조차 하지 않는다(성공하더라도 이미 Unloadable).
        factory.failNextCreate = false
        val third = cache.acquire(model)
        assertTrue("연속 2회 미완이면 그 이후는 시도조차 하지 않는다", third is RecognizerAcquireResult.Unloadable)
        assertEquals("세 번째 호출은 factory.create 까지 가면 안 된다(앞선 두 번만 호출됐어야 한다)", 2, factory.createCount)
    }

    @Test
    fun `a successful load clears the loading marker so a later failure is treated as the first, not the second`() {
        val factory = FakeSpeechRecognizerFactory()
        val cache = cache(factory)
        val model = model(CaptionLanguage.ENGLISH)

        cache.acquire(model) // 성공 — .loading 삭제
        cache.release(CaptionLanguage.ENGLISH)
        scheduler.advanceTimeBy(30_000)
        scheduler.runCurrent()

        factory.failNextCreate = true
        val afterCrash = cache.acquire(model)
        assertTrue(
            "직전 성공이 카운트를 지웠으니 이번 실패는 연속 2회째가 아니라 첫 실패다",
            afterCrash is RecognizerAcquireResult.Failed,
        )

        // 이전 성공이 카운트를 지워 놓았으니, 이번 한 번의 실패만으로는 영구 차단되지 않는다.
        val retry = cache.acquire(model)
        assertTrue("직전 성공 기록이 있으면 실패 한 번만으로 영구 차단되면 안 된다", retry is RecognizerAcquireResult.Acquired)
    }
}

private class FakeSpeechRecognizerFactory(var failNextCreate: Boolean = false) : SpeechRecognizerFactory {
    var createCount = 0
        private set

    override fun create(model: InstalledCaptionModel): SpeechRecognizer {
        createCount++
        if (failNextCreate) {
            failNextCreate = false
            error("모델 로드 실패(가짜)")
        }
        return FakeSpeechRecognizer()
    }
}

private class FakeSpeechRecognizer : SpeechRecognizer {
    var closed = false
        private set

    override fun createStream(sampleRateHz: Int): RecognitionStream = error("이 테스트에서는 쓰지 않는다")

    override fun close() {
        closed = true
    }
}
