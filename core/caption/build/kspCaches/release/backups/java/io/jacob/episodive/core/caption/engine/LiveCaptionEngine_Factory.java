package io.jacob.episodive.core.caption.engine;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.jacob.episodive.core.player.audio.SpeechPcmSource;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineDispatcher;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("io.jacob.episodive.core.caption.engine.CaptionThread")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class LiveCaptionEngine_Factory implements Factory<LiveCaptionEngine> {
  private final Provider<SpeechPcmSource> pcmProvider;

  private final Provider<RecognizerCache> recognizersProvider;

  private final Provider<CaptionTranslatorFactory> translatorsProvider;

  private final Provider<DeviceLanguageProvider> deviceLanguageProvider;

  private final Provider<CoroutineDispatcher> dispatcherProvider;

  private final Provider<CaptionClock> clockProvider;

  private LiveCaptionEngine_Factory(Provider<SpeechPcmSource> pcmProvider,
      Provider<RecognizerCache> recognizersProvider,
      Provider<CaptionTranslatorFactory> translatorsProvider,
      Provider<DeviceLanguageProvider> deviceLanguageProvider,
      Provider<CoroutineDispatcher> dispatcherProvider, Provider<CaptionClock> clockProvider) {
    this.pcmProvider = pcmProvider;
    this.recognizersProvider = recognizersProvider;
    this.translatorsProvider = translatorsProvider;
    this.deviceLanguageProvider = deviceLanguageProvider;
    this.dispatcherProvider = dispatcherProvider;
    this.clockProvider = clockProvider;
  }

  @Override
  public LiveCaptionEngine get() {
    return newInstance(pcmProvider.get(), recognizersProvider.get(), translatorsProvider.get(), deviceLanguageProvider.get(), dispatcherProvider.get(), clockProvider.get());
  }

  public static LiveCaptionEngine_Factory create(Provider<SpeechPcmSource> pcmProvider,
      Provider<RecognizerCache> recognizersProvider,
      Provider<CaptionTranslatorFactory> translatorsProvider,
      Provider<DeviceLanguageProvider> deviceLanguageProvider,
      Provider<CoroutineDispatcher> dispatcherProvider, Provider<CaptionClock> clockProvider) {
    return new LiveCaptionEngine_Factory(pcmProvider, recognizersProvider, translatorsProvider, deviceLanguageProvider, dispatcherProvider, clockProvider);
  }

  public static LiveCaptionEngine newInstance(SpeechPcmSource pcm, RecognizerCache recognizers,
      CaptionTranslatorFactory translators, DeviceLanguageProvider deviceLanguage,
      CoroutineDispatcher dispatcher, CaptionClock clock) {
    return new LiveCaptionEngine(pcm, recognizers, translators, deviceLanguage, dispatcher, clock);
  }
}
