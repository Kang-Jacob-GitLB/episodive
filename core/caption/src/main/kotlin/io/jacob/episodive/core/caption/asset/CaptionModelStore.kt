package io.jacob.episodive.core.caption.asset

import io.jacob.episodive.core.model.caption.CaptionLanguage
import java.io.File

/**
 * STT 모델 파일의 설치 여부를 판정하고 저장 디렉터리를 관리한다.
 *
 * 설치 판정은 "최종 파일이 전부 있고 크기가 스펙과 같다"까지만 본다 — 재해시하지 않는다.
 * 수백 MB 짜리 파일을 앱을 켤 때마다 통째로 훑을 이유가 없고, 손상 검증은 다운로드 시점의
 * sha256 확인으로 이미 끝냈다는 전제다.
 *
 * [rootDirectory] 는 실제로는 `File(context.noBackupFilesDir, "caption-models")` (`di/CaptionModule`).
 */
class CaptionModelStore(
    private val rootDirectory: File,
    private val specs: List<CaptionModelSpec> = CaptionModelRegistry.specs,
) {

    /** `<root>/<spec.id>/` — 언어별 모델 파일이 모이는 디렉터리. */
    fun directory(spec: CaptionModelSpec): File = File(rootDirectory, spec.id)

    /** 완성된 최종 파일 경로(다운로드 중에는 존재하지 않을 수 있다). */
    fun file(spec: CaptionModelSpec, asset: CaptionAssetFile): File = File(directory(spec), asset.fileName)

    /** 이어받기용 임시 파일 경로. */
    fun partFile(spec: CaptionModelSpec, asset: CaptionAssetFile): File =
        File(directory(spec), "${asset.fileName}.part")

    fun isAssetInstalled(spec: CaptionModelSpec, asset: CaptionAssetFile): Boolean {
        val target = file(spec, asset)
        return target.isFile && target.length() == asset.sizeBytes
    }

    fun isInstalled(spec: CaptionModelSpec): Boolean = spec.files.all { isAssetInstalled(spec, it) }

    /** 설치가 끝난 모델. 파일 하나라도 없거나 크기가 다르면 null. */
    fun installed(language: CaptionLanguage): InstalledCaptionModel? {
        val spec = specs.find { it.language == language } ?: return null
        if (!isInstalled(spec)) return null
        return InstalledCaptionModel(
            language = spec.language,
            encoderPath = file(spec, spec.encoder).absolutePath,
            decoderPath = file(spec, spec.decoder).absolutePath,
            joinerPath = file(spec, spec.joiner).absolutePath,
            tokensPath = file(spec, spec.tokens).absolutePath,
            maxLineChars = spec.maxLineChars,
        )
    }

    /**
     * 이미 받아 둔 바이트(완성 파일 + 이어받기용 `.part`)의 합. 앱을 재시작한 뒤 다운로드를
     * 재개할 때 진행률 초깃값으로 쓴다.
     */
    fun downloadedBytes(spec: CaptionModelSpec): Long =
        spec.files.sumOf { asset ->
            if (isAssetInstalled(spec, asset)) {
                asset.sizeBytes
            } else {
                partFile(spec, asset).let { if (it.isFile) it.length() else 0L }
            }
        }

    /** 레지스트리에서 빠진 언어의 디렉터리를 지운다(구버전이 받아 둔 모델 등). 관리자 초기화 시 호출. */
    fun pruneUnregistered() {
        val known = specs.map { it.id }.toSet()
        rootDirectory.listFiles()?.forEach { dir ->
            if (dir.isDirectory && dir.name !in known) dir.deleteRecursively()
        }
    }

    /** 해당 언어의 모델 디렉터리를 통째로 지운다(설치본 + 진행 중이던 `.part` 포함). */
    fun invalidate(language: CaptionLanguage) {
        val spec = specs.find { it.language == language } ?: return
        directory(spec).deleteRecursively()
    }
}
