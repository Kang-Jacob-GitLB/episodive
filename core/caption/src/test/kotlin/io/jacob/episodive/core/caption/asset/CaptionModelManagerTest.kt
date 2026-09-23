package io.jacob.episodive.core.caption.asset

import app.cash.turbine.test
import io.jacob.episodive.core.model.caption.CaptionDownloadFailure
import io.jacob.episodive.core.model.caption.CaptionLanguage
import io.jacob.episodive.core.model.caption.CaptionModelState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

class CaptionModelManagerTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val specs = listOf(CaptionAssetTestData.spec, CaptionAssetTestData.otherSpec)
    private val server = MockWebServer()

    private lateinit var root: File
    private lateinit var store: CaptionModelStore
    private lateinit var scope: CoroutineScope

    private fun redirectingClient(): OkHttpClient {
        val base = server.url("/")
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val redirected = original.url.newBuilder()
                    .scheme(base.scheme)
                    .host(base.host)
                    .port(base.port)
                    .build()
                chain.proceed(original.newBuilder().url(redirected).build())
            }
            .build()
    }

    private fun newManager(): CaptionModelManager =
        CaptionModelManager(store, CaptionModelDownloader(store, redirectingClient(), Dispatchers.IO), scope, specs)

    private fun enqueueSpecResponses(spec: CaptionModelSpec) {
        spec.files.forEach { asset ->
            server.enqueue(MockResponse().setBody(Buffer().write(CaptionAssetTestData.contentFor(spec, asset))))
        }
    }

    @Before
    fun setUp() {
        server.start()
        root = tmpFolder.newFolder("caption-models")
        store = CaptionModelStore(root, specs)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @After
    fun tearDown() {
        scope.cancel()
        server.shutdown()
    }

    @Test
    fun `이미 설치된 언어는 초기 상태가 Installed 다`() = runBlocking {
        CaptionAssetTestData.writeInstalled(store, CaptionAssetTestData.spec)

        val manager = newManager()

        assertEquals(CaptionModelState.Installed, manager.state(CaptionLanguage.ENGLISH).value)
        assertEquals(
            CaptionModelState.NotInstalled(CaptionAssetTestData.otherSpec.totalSizeBytes),
            manager.state(CaptionLanguage.KOREAN).value,
        )
    }

    @Test
    fun `start 를 두 번 불러도 다운로드 요청은 한 번만 나간다`() = runBlocking {
        val spec = CaptionAssetTestData.spec
        enqueueSpecResponses(spec)
        val manager = newManager()

        manager.start(spec.language)
        manager.start(spec.language)

        withTimeout(5.seconds) {
            manager.state(spec.language).first { it == CaptionModelState.Installed }
        }
        assertEquals(spec.files.size, server.requestCount)
    }

    @Test
    fun `invalidate 는 설치본을 지우고 NotInstalled 로 되돌린다`() = runBlocking {
        val spec = CaptionAssetTestData.spec
        CaptionAssetTestData.writeInstalled(store, spec)
        val manager = newManager()
        assertEquals(CaptionModelState.Installed, manager.state(spec.language).value)

        manager.invalidate(spec.language)

        assertEquals(CaptionModelState.NotInstalled(spec.totalSizeBytes), manager.state(spec.language).value)
        assertNull(store.installed(spec.language))
        assertFalse(store.directory(spec).exists())
    }

    @Test
    fun `다운로드 진행률은 1퍼센트 단위로만 방출되고 단조 증가한다`() = runBlocking {
        val spec = CaptionAssetTestData.spec
        enqueueSpecResponses(spec)
        val manager = newManager()

        val percents = mutableListOf<Float>()
        manager.state(spec.language).test(timeout = 5.seconds) {
            assertEquals(CaptionModelState.NotInstalled(spec.totalSizeBytes), awaitItem())

            manager.start(spec.language)

            while (true) {
                when (val state = awaitItem()) {
                    is CaptionModelState.Downloading -> percents += state.progress
                    CaptionModelState.Installed -> break
                    is CaptionModelState.NotInstalled -> error("unexpected regression to $state")
                }
            }
            cancelAndIgnoreRemainingEvents()
        }

        assertTrue("적어도 한 번은 진행률이 보고돼야 한다", percents.isNotEmpty())
        assertTrue("진행률은 감소하면 안 된다", percents.zipWithNext().all { (a, b) -> b >= a })
        // percent 가 바뀔 때만 방출한다는 계약: 연속된 값이 같으면 애초에 새 상태를 만들지
        // 않으므로(Manager.percentOf 게이트) 여기 모인 값 자체에 중복이 없어야 한다.
        assertEquals(percents.size, percents.distinct().size)
    }

    @Test
    fun `취소 직후 시작한 새 job 은 이전 job 의 늦은 finally 에 지워지지 않는다`() = runBlocking {
        // encoder 하나를 천천히(500ms 상당) 흘려 취소할 시간을 번다.
        fun enqueueSlowEncoder(spec: CaptionModelSpec) {
            server.enqueue(
                MockResponse()
                    .setBody(Buffer().write(ByteArray(spec.encoder.sizeBytes.toInt())))
                    .throttleBody(4, 50, TimeUnit.MILLISECONDS),
            )
        }

        val spec = CaptionAssetTestData.spec
        val manager = newManager()

        enqueueSlowEncoder(spec)
        manager.start(spec.language)
        withTimeout(5.seconds) {
            manager.state(spec.language).first { it is CaptionModelState.Downloading }
        }
        manager.cancel(spec.language)
        assertEquals(CaptionModelState.NotInstalled(spec.totalSizeBytes), manager.state(spec.language).value)

        // 취소된 job 이 call.cancel() 로 깨어나 finally 를 처리할 시간을 준다. 고친 코드는
        // jobs.remove(language, thisJob) 로 자기 항목만 지우니 문제가 없어야 하지만,
        // jobs.remove(language) 로 되돌리면 이 시간 동안 아래에서 새로 시작하는 job2 의
        // 항목을 지워 버린다.
        delay(200)

        enqueueSlowEncoder(spec)
        manager.start(spec.language)
        withTimeout(5.seconds) {
            manager.state(spec.language).first { it is CaptionModelState.Downloading }
        }

        // job1 이 job2 의 jobs 항목을 지웠다면 여기서 취소는 아무 job 도 찾지 못해 아무 일도
        // 일어나지 않고, job2 는 계속 돌아 결국 Installed 로 끝난다.
        manager.cancel(spec.language)

        assertEquals(CaptionModelState.NotInstalled(spec.totalSizeBytes), manager.state(spec.language).value)
        delay(300)
        assertEquals(
            "job2 가 취소되지 않고 계속 돌았다면 여기서 Installed 로 바뀌어 있다",
            CaptionModelState.NotInstalled(spec.totalSizeBytes),
            manager.state(spec.language).value,
        )
    }

    @Test
    fun `reportUnloadable 은 설치본을 지우고 NotInstalled 로 되돌리며 실패를 알린다`() = runBlocking {
        val spec = CaptionAssetTestData.spec
        CaptionAssetTestData.writeInstalled(store, spec)
        val manager = newManager()
        assertEquals(CaptionModelState.Installed, manager.state(spec.language).value)

        manager.downloadFailures.test(timeout = 5.seconds) {
            manager.reportUnloadable(spec.language)

            assertEquals(
                CaptionDownloadFailure(spec.language, CaptionDownloadFailure.Reason.UNLOADABLE),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(CaptionModelState.NotInstalled(spec.totalSizeBytes), manager.state(spec.language).value)
    }
}
