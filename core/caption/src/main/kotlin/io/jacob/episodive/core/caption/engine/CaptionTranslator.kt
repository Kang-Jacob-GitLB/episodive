package io.jacob.episodive.core.caption.engine

/** ML Kit 번역기를 만든다. 원문/대상 언어가 같거나 ML Kit 이 지원하지 않으면 null. */
interface CaptionTranslatorFactory {
    fun create(sourceTag: String, targetTag: String): CaptionTranslator?
}

/** 언어쌍 하나에 대한 번역기. */
interface CaptionTranslator : AutoCloseable {
    /** 온디바이스 모델을 받는다(`downloadModelIfNeeded`). 성공하면 true. */
    suspend fun prepare(): Boolean

    /** 준비 전이거나 실패하면 null. */
    suspend fun translate(text: String): String?
}

/** 기기의 현재 언어 태그(BCP-47)를 제공한다. */
fun interface DeviceLanguageProvider {
    fun languageTag(): String
}
