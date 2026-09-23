package io.jacob.episodive.core.caption.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.jacob.episodive.core.caption.engine.CaptionTranslatorFactory
import io.jacob.episodive.core.caption.engine.DeviceLanguageProvider
import io.jacob.episodive.core.caption.engine.LocaleDeviceLanguageProvider
import io.jacob.episodive.core.caption.engine.SpeechRecognizerFactory
import io.jacob.episodive.core.caption.mlkit.MlKitCaptionTranslatorFactory
import io.jacob.episodive.core.caption.sherpa.SherpaSpeechRecognizerFactory
import javax.inject.Singleton

/** 인터페이스 ↔ 어댑터 바인딩만 모은다. 값을 만드는 조립(OkHttpClient·디스패처 등)은 [CaptionModule]. */
@Module
@InstallIn(SingletonComponent::class)
abstract class CaptionBindingModule {
    @Binds
    @Singleton
    abstract fun bindSpeechRecognizerFactory(impl: SherpaSpeechRecognizerFactory): SpeechRecognizerFactory

    @Binds
    @Singleton
    abstract fun bindCaptionTranslatorFactory(impl: MlKitCaptionTranslatorFactory): CaptionTranslatorFactory

    @Binds
    @Singleton
    abstract fun bindDeviceLanguageProvider(impl: LocaleDeviceLanguageProvider): DeviceLanguageProvider
}
