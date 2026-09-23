package io.jacob.episodive.core.caption.asset

import io.jacob.episodive.core.model.caption.CaptionDownloadFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
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

class CaptionModelDownloaderTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val server = MockWebServer()
    private lateinit var root: File
    private lateinit var store: CaptionModelStore
    private lateinit var downloader: CaptionModelDownloader

    /** 모든 asset 이 huggingface.co 절대 URL 을 가리키므로, 경로만 남기고 목 서버로 리다이렉트한다. */
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

    @Before
    fun setUp() {
        server.start()
        root = tmpFolder.newFolder("caption-models")
        store = CaptionModelStore(root, listOf(CaptionAssetTestData.spec))
        downloader = CaptionModelDownloader(store, redirectingClient(), Dispatchers.IO)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    /** encoder/decoder/joiner 는 미리 설치해 두어, 테스트마다 tokens 파일 하나만 오가게 한다. */
    private fun preinstallAllButTokens(spec: CaptionModelSpec) {
        store.directory(spec).mkdirs()
        store.file(spec, spec.encoder).writeBytes(CaptionAssetTestData.contentFor(spec, spec.encoder))
        store.file(spec, spec.decoder).writeBytes(CaptionAssetTestData.contentFor(spec, spec.decoder))
        store.file(spec, spec.joiner).writeBytes(CaptionAssetTestData.contentFor(spec, spec.joiner))
    }

    @Test
    fun `정상 응답은 최종 파일로 rename 된다`() = runBlocking {
        val spec = CaptionAssetTestData.spec
        preinstallAllButTokens(spec)
        server.enqueue(MockResponse().setBody(Buffer().write(CaptionAssetTestData.tokensContent)))

        val writtenDeltas = mutableListOf<Long>()
        val reason = downloader.download(spec) { delta -> writtenDeltas += delta }

        assertNull(reason)
        assertArrayEquals(CaptionAssetTestData.tokensContent, store.file(spec, spec.tokens).readBytes())
        assertFalse(store.partFile(spec, spec.tokens).exists())
        assertEquals(CaptionAssetTestData.tokensContent.size.toLong(), writtenDeltas.sum())
    }

    @Test
    fun `크기가 다르면 CORRUPT 이고 최종 파일이 없다`() = runBlocking {
        val spec = CaptionAssetTestData.spec
        preinstallAllButTokens(spec)
        server.enqueue(MockResponse().setBody(Buffer().write(CaptionAssetTestData.tokensContent + byteArrayOf(0))))

        val reason = downloader.download(spec) { }

        assertEquals(CaptionDownloadFailure.Reason.CORRUPT, reason)
        assertFalse(store.file(spec, spec.tokens).exists())
        assertFalse(store.partFile(spec, spec.tokens).exists())
    }

    @Test
    fun `크기는 같지만 내용이 다르면 CORRUPT 이고 최종 파일이 없다`() = runBlocking {
        val spec = CaptionAssetTestData.spec
        preinstallAllButTokens(spec)
        val corrupted = CaptionAssetTestData.tokensContent.copyOf().also { it[0] = it[0].inc() }
        server.enqueue(MockResponse().setBody(Buffer().write(corrupted)))

        val reason = downloader.download(spec) { }

        assertEquals(CaptionDownloadFailure.Reason.CORRUPT, reason)
        assertFalse(store.file(spec, spec.tokens).exists())
    }

    @Test
    fun `이어받을 부분이 있으면 Range 헤더로 요청한다`() = runBlocking {
        val spec = CaptionAssetTestData.spec
        preinstallAllButTokens(spec)
        val full = CaptionAssetTestData.tokensContent
        val alreadyHave = full.copyOfRange(0, 4)
        store.partFile(spec, spec.tokens).writeBytes(alreadyHave)
        server.enqueue(
            MockResponse()
                .setResponseCode(206)
                .setBody(Buffer().write(full.copyOfRange(4, full.size))),
        )

        val reason = downloader.download(spec) { }

        val request = server.takeRequest(1, TimeUnit.SECONDS)
        assertEquals("bytes=4-", request?.getHeader("Range"))
        assertNull(reason)
        assertArrayEquals(full, store.file(spec, spec.tokens).readBytes())
    }

    @Test
    fun `서버가 Range 를 무시하고 200 을 주면 처음부터 다시 받는다`() = runBlocking {
        val spec = CaptionAssetTestData.spec
        preinstallAllButTokens(spec)
        val full = CaptionAssetTestData.tokensContent
        // 이전 시도가 남긴 부분이 실제 접두부가 아니라고 가정(서버가 그 사이 파일을 바꿨거나
        // 손상됐던 상황) — Range 를 무시한 200 응답이면 이 잔해를 버리고 처음부터 받아야 한다.
        store.partFile(spec, spec.tokens).writeBytes(byteArrayOf(9, 9, 9, 9))
        server.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(full)))

        val reason = downloader.download(spec) { }

        assertNull(reason)
        assertArrayEquals(full, store.file(spec, spec.tokens).readBytes())
    }

    @Test
    fun `part 파일이 이미 목표 크기 이상이면 네트워크 없이 검증만 하고 rename 한다`() = runBlocking {
        val spec = CaptionAssetTestData.spec
        preinstallAllButTokens(spec)
        // 이전 시도가 rename 직전에 죽어 .part 는 이미 완성된 내용을 담고 있다.
        store.partFile(spec, spec.tokens).writeBytes(CaptionAssetTestData.tokensContent)
        // 서버 응답을 하나도 enqueue 하지 않았다 — 요청이 가면 MockWebServer 가 예외를 던진다.

        val reason = downloader.download(spec) { }

        assertNull(reason)
        assertArrayEquals(CaptionAssetTestData.tokensContent, store.file(spec, spec.tokens).readBytes())
        assertFalse(store.partFile(spec, spec.tokens).exists())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `크기는 넘지만 내용이 틀린 part 는 버리고 처음부터 다시 받는다`() = runBlocking {
        val spec = CaptionAssetTestData.spec
        preinstallAllButTokens(spec)
        // 목표 크기 이상이지만 내용이 다른 잔해 — 검증에 실패해야 한다.
        store.partFile(spec, spec.tokens).writeBytes(ByteArray(spec.tokens.sizeBytes.toInt() + 4) { 9 })
        server.enqueue(MockResponse().setBody(Buffer().write(CaptionAssetTestData.tokensContent)))

        val reason = downloader.download(spec) { }

        assertNull(reason)
        assertArrayEquals(CaptionAssetTestData.tokensContent, store.file(spec, spec.tokens).readBytes())
        // 처음부터 다시 받은 것이니 Range 헤더 없이 요청했어야 한다.
        val request = server.takeRequest(1, TimeUnit.SECONDS)
        assertNull(request?.getHeader("Range"))
    }

    @Test
    fun `서버가 416 을 주면 part 파일을 지운다`() = runBlocking {
        val spec = CaptionAssetTestData.spec
        preinstallAllButTokens(spec)
        val alreadyHave = CaptionAssetTestData.tokensContent.copyOfRange(0, 4)
        store.partFile(spec, spec.tokens).writeBytes(alreadyHave)
        server.enqueue(MockResponse().setResponseCode(416))

        val reason = downloader.download(spec) { }

        assertEquals(CaptionDownloadFailure.Reason.NETWORK, reason)
        assertFalse(store.partFile(spec, spec.tokens).exists())
    }

    @Test
    fun `이어받기가 아니라 처음부터 다시 받으면 되돌림 델타를 흘려 이중 집계를 막는다`() = runBlocking {
        val spec = CaptionAssetTestData.spec
        preinstallAllButTokens(spec)
        // 이전 시도가 남긴 4바이트 — Range 를 무시한 200 응답이면 그만큼을 되돌려야 한다.
        store.partFile(spec, spec.tokens).writeBytes(byteArrayOf(9, 9, 9, 9))
        server.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(CaptionAssetTestData.tokensContent)))

        // Manager 는 시작할 때 store.downloadedBytes() 로 이 4바이트를 이미 downloaded 에
        // 반영해 둔다 — 그 초깃값을 흉내내 델타를 그대로 누적해 보면 되돌림이 실제로 이중
        // 집계를 막는지 검증할 수 있다.
        var downloaded = 4L
        val deltas = mutableListOf<Long>()
        val reason = downloader.download(spec) { delta ->
            deltas += delta
            downloaded += delta
        }

        assertNull(reason)
        assertTrue("음수 델타로 이전 집계를 되돌려야 한다", deltas.any { it < 0 })
        // 되돌림이 없었다면 4(이전 집계) + 8(이번에 새로 받은 전체) = 12 로, 실제 크기(8)를
        // 넘겨 100% 를 넘는 진행률을 만들었을 것이다.
        assertEquals(CaptionAssetTestData.tokensContent.size.toLong(), downloaded)
    }

    @Test
    fun `취소하면 part 파일이 남지 않는다`() = runBlocking {
        // 취소를 관찰할 시간을 주기 위해 이 테스트만 별도의 큼직한 스펙/서버 응답을 쓴다.
        val big = CaptionAssetTestData.spec.copy(
            tokens = CaptionAssetTestData.spec.tokens.copy(sizeBytes = 200_000L),
        )
        val bigStore = CaptionModelStore(root, listOf(big))
        val bigDownloader = CaptionModelDownloader(bigStore, redirectingClient(), Dispatchers.IO)
        server.enqueue(
            MockResponse()
                .setBody(Buffer().write(ByteArray(200_000)))
                .throttleBody(4_096, 20, TimeUnit.MILLISECONDS),
        )

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val job = scope.launch {
            bigDownloader.download(big) { }
        }
        // 스트리밍이 실제로 시작될 시간을 준 뒤 취소한다.
        delay(50)
        job.cancelAndJoin()
        scope.cancel()

        assertFalse(bigStore.partFile(big, big.tokens).exists())
        assertFalse(bigStore.file(big, big.tokens).exists())
    }

    @Test
    fun `네트워크 오류가 나면 파트 파일을 지우지 않는다`() = runBlocking {
        val spec = CaptionAssetTestData.spec
        preinstallAllButTokens(spec)
        val partial = CaptionAssetTestData.tokensContent.copyOfRange(0, 4)
        store.partFile(spec, spec.tokens).writeBytes(partial)
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val reason = withTimeout(5_000) { downloader.download(spec) { } }

        assertEquals(CaptionDownloadFailure.Reason.NETWORK, reason)
        assertArrayEquals(partial, store.partFile(spec, spec.tokens).readBytes())
        assertFalse(store.file(spec, spec.tokens).exists())
    }

    @Test
    fun `이미 완성된 파일은 다시 받지 않는다`() = runBlocking {
        val spec = CaptionAssetTestData.spec
        CaptionAssetTestData.writeInstalled(store, spec)
        // 서버 응답을 하나도 enqueue 하지 않았다 — 요청이 가면 MockWebServer 가 예외를 던진다.

        val reason = downloader.download(spec) { }

        assertNull(reason)
        assertEquals(0, server.requestCount)
    }
}
