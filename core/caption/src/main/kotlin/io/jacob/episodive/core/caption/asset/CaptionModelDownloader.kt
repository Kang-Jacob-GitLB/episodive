package io.jacob.episodive.core.caption.asset

import io.jacob.episodive.core.model.caption.CaptionDownloadFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

/**
 * STT 모델 파일을 Hugging Face 에서 내려받는다.
 *
 * `:core:network` 의 OkHttpClient 는 Podcast Index 인증 인터셉터가 실려 있어 그 키가 그대로
 * Hugging Face 로 샌다 — 반드시 이 클래스 전용 [httpClient] 를 주입받는다.
 */
class CaptionModelDownloader(
    private val store: CaptionModelStore,
    private val httpClient: OkHttpClient,
    private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * [spec] 을 이루는 파일을 순서대로 내려받는다. 이미 완성된 파일은 건너뛰고, `.part` 가
     * 있으면 이어받는다. 실패 사유를 반환하며 성공은 null. [onBytesWritten] 은 새로 쓴
     * 바이트 수(델타)로 호출된다 — 누적/퍼센트 환산은 호출부(Manager) 책임. 이어받기로 여겼던
     * `.part` 를 버리고 처음부터 다시 받게 되면(검증 실패, 서버가 Range 를 무시) 그만큼을
     * **음수 델타**로 한 번 흘려 Manager 의 누적을 되돌린다 — Manager 는 시작할 때 그 `.part`
     * 길이를 이미 downloaded 에 반영해 두었기 때문이다.
     *
     * 코루틴 취소는 그대로 전파한다. 취소·사용자 취소 모두 이 함수 안에서 `.part` 를 지운다.
     */
    suspend fun download(
        spec: CaptionModelSpec,
        onBytesWritten: (Long) -> Unit,
    ): CaptionDownloadFailure.Reason? = withContext(ioDispatcher) {
        val dir = store.directory(spec)
        dir.mkdirs()

        val pending = spec.files.filterNot { store.isAssetInstalled(spec, it) }
        if (pending.isEmpty()) return@withContext null

        val remainingBytes = pending.sumOf { asset ->
            val part = store.partFile(spec, asset)
            asset.sizeBytes - (if (part.isFile) part.length() else 0L)
        }
        if (dir.usableSpace < remainingBytes + STORAGE_HEADROOM_BYTES) {
            return@withContext CaptionDownloadFailure.Reason.STORAGE
        }

        for (asset in pending) {
            val reason = downloadAsset(dir, spec, asset, onBytesWritten)
            if (reason != null) return@withContext reason
        }
        null
    }

    private suspend fun downloadAsset(
        dir: File,
        spec: CaptionModelSpec,
        asset: CaptionAssetFile,
        onBytesWritten: (Long) -> Unit,
    ): CaptionDownloadFailure.Reason? {
        val target = store.file(spec, asset)
        val part = store.partFile(spec, asset)

        try {
            // 이미 받아 둔 조각이 목표 크기 이상이면(이전 시도가 rename 직전에 죽었거나, 서버가
            // Range 를 무시해 그새 전체를 다시 받아 둔 경우) 네트워크 없이 바로 검증한다.
            if (part.isFile && part.length() >= asset.sizeBytes) {
                if (part.length() == asset.sizeBytes && sha256Hex(part) == asset.sha256) {
                    return if (part.renameTo(target)) null else CaptionDownloadFailure.Reason.NETWORK
                }
                // 검증에 실패하면 CORRUPT 로 막지 않고 잔해를 버려 처음부터 다시 받게 한다.
                // Manager 는 시작할 때 store.downloadedBytes() 로 이 .part 를 이미 downloaded 에
                // 반영해 뒀다 — 지우기 전에 그만큼 되돌림 신호를 보내지 않으면 재다운로드분이
                // 더해질 때 실제보다 많이 받은 것처럼 보인다.
                onBytesWritten(-part.length())
                part.delete()
            }

            val digest = MessageDigest.getInstance("SHA-256")
            var resumeFrom = 0L
            if (part.isFile && part.length() > 0L) {
                // 이어받기 전에 이미 받아 둔 앞부분을 다이제스트에 먼저 먹인다. 부분 해시를 따로
                // 저장해 두지 않으므로, 완주했을 때 파일 전체의 sha256 을 한 번에 비교하려면
                // 여기서부터 이어 계산해야 한다.
                part.inputStream().use { input -> digest.consume(input) }
                resumeFrom = part.length()
            }

            val request = Request.Builder()
                .url(spec.assetUrl(asset))
                .apply { if (resumeFrom > 0L) header("Range", "bytes=$resumeFrom-") }
                .build()
            val context = currentCoroutineContext()

            // execute()/read() 는 블로킹이라 코루틴 취소에 스스로 반응하지 않는다.
            // executeCancellable 이 call.cancel() 을 취소 훅으로 걸어 두므로, 이 블로킹 구간이
            // 도는 동안 코루틴이 취소되면 연결이 즉시 끊기며 read() 가 IOException 으로 깨어난다
            // — 그리고 그 취소는 아래 호출 지점에서 CancellationException 으로 그대로 드러난다
            // (코루틴이 이미 취소된 continuation 을 resume 해도 결과는 취소로 확정되기 때문에,
            // 이 catch 블록들이 "취소로 인한 IOException" 을 따로 가려낼 필요가 없다).
            val reason = httpClient.newCall(request).executeCancellable { response ->
                if (response.code == 416) {
                    // 우리가 보낸 Range 가 파일 크기를 벗어났다는 뜻 — .part 를 못 믿을
                    // 상태다. 지우고 다음 시도가 처음부터 받게 한다.
                    part.delete()
                    return@executeCancellable CaptionDownloadFailure.Reason.NETWORK
                }
                if (!response.isSuccessful) return@executeCancellable CaptionDownloadFailure.Reason.NETWORK
                val body = response.body ?: return@executeCancellable CaptionDownloadFailure.Reason.NETWORK

                // 서버가 Range 를 무시하고 200 으로 답하면 처음부터 다시 받는다.
                val resumed = resumeFrom > 0L && response.code == 206
                if (resumeFrom > 0L && !resumed) {
                    // Manager 의 downloaded 는 이 .part 의 기존 길이(resumeFrom)를 이미
                    // 세어 뒀다 — 이어받기가 아니라 처음부터 다시 받는 것이니 그만큼 되돌린다.
                    onBytesWritten(-resumeFrom)
                    digest.reset()
                    resumeFrom = 0L
                }

                FileOutputStream(part, resumed).use { out ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                        while (context.isActive) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            out.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            onBytesWritten(read.toLong())
                        }
                    }
                }
                null
            }
            if (reason != null) return reason
            // 취소돼서 위 루프를 빠져나왔을 수 있다 — catch 로 넘겨 .part 를 지운다.
            context.ensureActive()

            if (part.length() != asset.sizeBytes || digest.hex() != asset.sha256) {
                part.delete()
                return CaptionDownloadFailure.Reason.CORRUPT
            }
            if (!part.renameTo(target)) {
                return CaptionDownloadFailure.Reason.NETWORK
            }
            return null
        } catch (e: CancellationException) {
            part.delete()
            throw e
        } catch (e: IOException) {
            // .part 는 그대로 둔다 — 다음 시도가 이어받을 수 있게.
            return CaptionDownloadFailure.Reason.NETWORK
        }
    }

    companion object {
        private const val DOWNLOAD_BUFFER_SIZE = 64 * 1024
        private const val STORAGE_HEADROOM_BYTES = 16L * 1024 * 1024
    }
}

/**
 * [Call.execute] 는 블로킹이라 코루틴 취소에 스스로 반응하지 않는다. 이 취소 훅을 걸어 두면
 * 코루틴이 [block] 실행 도중(같은 스레드에서 여전히 블로킹 중이어도) 취소될 때 다른 스레드에서
 * [Call.cancel] 이 불려 연결을 끊고, 블로킹 중이던 읽기가 IOException 으로 깨어난다.
 *
 * [suspendCancellableCoroutine] 의 표준 브리징 패턴이다: `block` 이 같은 스레드에서 동기로 끝까지
 * 돌고 그 안에서 바로 `resume` 하지만, 그 전에 등록해 둔 [invokeOnCancellation][kotlinx.coroutines.CancellableContinuation.invokeOnCancellation]
 * 이 취소를 비동기로 받아 [Call.cancel] 을 불러 준다.
 */
private suspend fun <T> Call.executeCancellable(block: (Response) -> T): T =
    suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { cancel() }
        cont.resumeWith(runCatching { execute().use(block) })
    }

private fun MessageDigest.consume(input: InputStream) {
    val buffer = ByteArray(64 * 1024)
    while (true) {
        val read = input.read(buffer)
        if (read <= 0) break
        update(buffer, 0, read)
    }
}

private fun MessageDigest.hex(): String = digest().joinToString("") { "%02x".format(it) }

private fun sha256Hex(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input -> digest.consume(input) }
    return digest.hex()
}
