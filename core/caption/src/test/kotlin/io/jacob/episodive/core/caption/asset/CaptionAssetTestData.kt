package io.jacob.episodive.core.caption.asset

import io.jacob.episodive.core.model.caption.CaptionLanguage
import java.security.MessageDigest

/**
 * asset 패키지 테스트 전용 소형 스펙. 실제 레지스트리(파일당 수십~수백 MB)를 그대로 쓰면
 * 테스트가 그만한 바이트를 실제로 디스크/네트워크로 오가야 해서 느리고 무겁다 — 파일마다
 * 몇십 바이트인 가짜 스펙을 대신 쓴다. `:core:testing` 팩토리로 옮기지 않는 이유는 CLAUDE.md
 * 규약대로 domain/DB 테스트 데이터가 아니라 이 모듈의 다운로드·저장 로직에만 쓰는 내부
 * 픽스처이기 때문이다.
 */
object CaptionAssetTestData {

    private fun content(seed: Int, sizeBytes: Int): ByteArray = ByteArray(sizeBytes) { index -> (seed + index).toByte() }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun asset(fileName: String, content: ByteArray): CaptionAssetFile =
        CaptionAssetFile(fileName = fileName, sizeBytes = content.size.toLong(), sha256 = sha256Hex(content))

    val encoderContent: ByteArray = content(seed = 1, sizeBytes = 40)
    val decoderContent: ByteArray = content(seed = 2, sizeBytes = 24)
    val joinerContent: ByteArray = content(seed = 3, sizeBytes = 16)
    val tokensContent: ByteArray = content(seed = 4, sizeBytes = 8)

    val spec: CaptionModelSpec = CaptionModelSpec(
        language = CaptionLanguage.ENGLISH,
        repo = "test/repo",
        revision = "deadbeef",
        encoder = asset("encoder.onnx", encoderContent),
        decoder = asset("decoder.onnx", decoderContent),
        joiner = asset("joiner.onnx", joinerContent),
        tokens = asset("tokens.txt", tokensContent),
        maxLineChars = 40,
    )

    val otherSpec: CaptionModelSpec = spec.copy(language = CaptionLanguage.KOREAN)

    /** [spec] 의 각 asset 이 담아야 할 올바른 본문. MockWebServer 응답 본문을 만들 때 쓴다. */
    fun contentFor(spec: CaptionModelSpec, asset: CaptionAssetFile): ByteArray = when (asset.fileName) {
        spec.encoder.fileName -> encoderContent
        spec.decoder.fileName -> decoderContent
        spec.joiner.fileName -> joinerContent
        spec.tokens.fileName -> tokensContent
        else -> error("unknown asset ${asset.fileName}")
    }

    /** [store] 에 [spec] 의 모든 최종 파일을 올바른 내용으로 써 둔다(설치된 상태를 만든다). */
    fun writeInstalled(store: CaptionModelStore, spec: CaptionModelSpec) {
        store.directory(spec).mkdirs()
        spec.files.forEach { asset -> store.file(spec, asset).writeBytes(contentFor(spec, asset)) }
    }
}
