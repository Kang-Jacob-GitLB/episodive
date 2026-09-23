package io.jacob.episodive.core.caption.engine;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
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
public final class RecognizerCache_Factory implements Factory<RecognizerCache> {
  private final Provider<SpeechRecognizerFactory> factoryProvider;

  private final Provider<CoroutineDispatcher> dispatcherProvider;

  private RecognizerCache_Factory(Provider<SpeechRecognizerFactory> factoryProvider,
      Provider<CoroutineDispatcher> dispatcherProvider) {
    this.factoryProvider = factoryProvider;
    this.dispatcherProvider = dispatcherProvider;
  }

  @Override
  public RecognizerCache get() {
    return newInstance(factoryProvider.get(), dispatcherProvider.get());
  }

  public static RecognizerCache_Factory create(Provider<SpeechRecognizerFactory> factoryProvider,
      Provider<CoroutineDispatcher> dispatcherProvider) {
    return new RecognizerCache_Factory(factoryProvider, dispatcherProvider);
  }

  public static RecognizerCache newInstance(SpeechRecognizerFactory factory,
      CoroutineDispatcher dispatcher) {
    return new RecognizerCache(factory, dispatcher);
  }
}
