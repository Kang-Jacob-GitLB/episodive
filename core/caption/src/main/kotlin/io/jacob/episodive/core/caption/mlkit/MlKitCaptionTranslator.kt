package io.jacob.episodive.core.caption.mlkit

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslatorOptions
import io.jacob.episodive.core.caption.engine.CaptionTranslator
import io.jacob.episodive.core.caption.engine.CaptionTranslatorFactory
import io.jacob.episodive.core.caption.text.CaptionTranslationCache
import io.jacob.episodive.core.caption.text.toPrimaryLanguageSubtag
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

/**
 * ML Kit 온디바이스 번역기를 만든다. 태그 정규화·언어 매핑 판정은 [toPrimaryLanguageSubtag] /
 * `TranslateLanguage.fromLanguageTag` 로 순수하게 갈라내고, 이 클래스는 그 결과로 ML Kit
 * 클라이언트를 올리는 어댑터 역할만 한다(JVM 테스트 대상 밖 — `mlkit` 패키지는 Kover 제외).
 */
class MlKitCaptionTranslatorFactory @Inject constructor() : CaptionTranslatorFactory {

    override fun create(sourceTag: String, targetTag: String): CaptionTranslator? {
        val source = sourceTag.toPrimaryLanguageSubtag()?.let(TranslateLanguage::fromLanguageTag)
        val target = targetTag.toPrimaryLanguageSubtag()?.let(TranslateLanguage::fromLanguageTag)
        if (source == null || target == null || source == target) return null

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
        return MlKitCaptionTranslator(Translation.getClient(options))
    }
}

private class MlKitCaptionTranslator(
    private val client: com.google.mlkit.nl.translate.Translator,
) : CaptionTranslator {

    private var prepared = false
    private val cache = CaptionTranslationCache()

    override suspend fun prepare(): Boolean {
        prepared = try {
            client.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            true
        } catch (e: Exception) {
            Timber.w(e, "caption translation model download failed")
            false
        }
        return prepared
    }

    override suspend fun translate(text: String): String? {
        if (!prepared || text.isEmpty()) return null
        cache.get(text)?.let { return it }

        return try {
            client.translate(text).await().also { cache.put(text, it) }
        } catch (e: Exception) {
            Timber.w(e, "caption translation failed")
            null
        }
    }

    override fun close() = client.close()
}
