package io.jacob.episodive.core.caption.asset

import io.jacob.episodive.core.model.caption.CaptionLanguage

/**
 * STT 모델을 이루는 파일 하나(encoder/decoder/joiner/tokens). Hugging Face 에서
 * `https://huggingface.co/{repo}/resolve/{revision}/{fileName}` 로 내려받는다.
 */
data class CaptionAssetFile(
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
)

/**
 * 언어별 STT 모델 레지스트리 항목. `revision` 을 고정해 원격 콘텐츠가 바뀌어도
 * sha256 검증이 배신하지 않게 한다.
 */
data class CaptionModelSpec(
    val language: CaptionLanguage,
    val repo: String,
    val revision: String,
    val encoder: CaptionAssetFile,
    val decoder: CaptionAssetFile,
    val joiner: CaptionAssetFile,
    val tokens: CaptionAssetFile,
    val maxLineChars: Int,
) {
    /** `<noBackupFilesDir>/caption-models/<id>/` 저장 디렉터리 이름으로 쓴다. */
    val id: String get() = language.value

    val files: List<CaptionAssetFile> get() = listOf(encoder, decoder, joiner, tokens)

    val totalSizeBytes: Long get() = files.sumOf { it.sizeBytes }

    fun assetUrl(file: CaptionAssetFile): String =
        "https://huggingface.co/$repo/resolve/$revision/${file.fileName}"
}

/** 사용자 확정 언어(영어/한국어) 스트리밍 zipformer int8 모델 레지스트리. */
object CaptionModelRegistry {
    val ENGLISH = CaptionModelSpec(
        language = CaptionLanguage.ENGLISH,
        repo = "csukuangfj/sherpa-onnx-streaming-zipformer-en-2023-06-26",
        revision = "672fbf1b30579d6585301139bb363f42a0ad4a24",
        encoder = CaptionAssetFile(
            fileName = "encoder-epoch-99-avg-1-chunk-16-left-128.int8.onnx",
            sizeBytes = 71_083_163L,
            sha256 = "563fde436d16cf7607cf408cd6b30909819d03162652ef389c2450ced3f45ac1",
        ),
        decoder = CaptionAssetFile(
            fileName = "decoder-epoch-99-avg-1-chunk-16-left-128.int8.onnx",
            sizeBytes = 1_307_236L,
            sha256 = "98da299f471e38bb4e1a8df579b8cc9122d6039576a77e357b3c60f17dd83b02",
        ),
        joiner = CaptionAssetFile(
            fileName = "joiner-epoch-99-avg-1-chunk-16-left-128.int8.onnx",
            sizeBytes = 259_335L,
            sha256 = "d944208d660d67c8d72cd2acaeac971fa5ceb8c80e76c1968148846fedd6e297",
        ),
        tokens = CaptionAssetFile(
            fileName = "tokens.txt",
            sizeBytes = 5_048L,
            sha256 = "49e3c2646595fd907228b3c6787069658f67b17377c60aeb8619c4551b2316fb",
        ),
        maxLineChars = 80,
    )

    val KOREAN = CaptionModelSpec(
        language = CaptionLanguage.KOREAN,
        repo = "k2-fsa/sherpa-onnx-streaming-zipformer-korean-2024-06-16",
        revision = "ba6078bca4daf3f0dd37f79d0ab505af71df14a6",
        encoder = CaptionAssetFile(
            fileName = "encoder-epoch-99-avg-1.int8.onnx",
            sizeBytes = 126_968_852L,
            sha256 = "8d0b1aa24fbedd4e3948564ab7facd151b8ce9b0c48fc987c541de2de3af5697",
        ),
        decoder = CaptionAssetFile(
            fileName = "decoder-epoch-99-avg-1.int8.onnx",
            sizeBytes = 2_844_692L,
            sha256 = "68ea197936aabd249f38b53a87c775422bca64428ad4427d0e6e8092593e71fb",
        ),
        joiner = CaptionAssetFile(
            fileName = "joiner-epoch-99-avg-1.int8.onnx",
            sizeBytes = 2_581_421L,
            sha256 = "128b80a66a1f718488af8560f9d15895109b99ff3e573f0a0130e03774ef1ced",
        ),
        tokens = CaptionAssetFile(
            fileName = "tokens.txt",
            sizeBytes = 60_246L,
            sha256 = "016bdf0965029263b7ad01b742366ee542ef0bef38261510e8176ff6f2e9e668",
        ),
        maxLineChars = 40,
    )

    /** 저장소 정리·초기 상태 판정처럼 언어를 미리 모를 때(전 스펙 순회) 쓴다. */
    val specs: List<CaptionModelSpec> = listOf(ENGLISH, KOREAN)

    fun spec(language: CaptionLanguage): CaptionModelSpec =
        specs.find { it.language == language }
            ?: error("caption model spec not registered for $language")
}

/**
 * 설치가 끝난 STT 모델. 파일 경로는 전부 절대경로(`<noBackupFilesDir>/caption-models/<id>/...`).
 */
data class InstalledCaptionModel(
    val language: CaptionLanguage,
    val encoderPath: String,
    val decoderPath: String,
    val joinerPath: String,
    val tokensPath: String,
    val maxLineChars: Int,
)
