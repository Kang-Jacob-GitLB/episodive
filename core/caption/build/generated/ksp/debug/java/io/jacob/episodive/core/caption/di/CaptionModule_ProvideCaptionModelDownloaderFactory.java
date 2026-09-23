package io.jacob.episodive.core.caption.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.jacob.episodive.core.caption.asset.CaptionModelDownloader;
import io.jacob.episodive.core.caption.asset.CaptionModelStore;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineDispatcher;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata({
    "io.jacob.episodive.core.caption.di.CaptionOkHttpClient",
    "io.jacob.episodive.core.common.Dispatcher"
})
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
public final class CaptionModule_ProvideCaptionModelDownloaderFactory implements Factory<CaptionModelDownloader> {
  private final Provider<CaptionModelStore> storeProvider;

  private final Provider<OkHttpClient> httpClientProvider;

  private final Provider<CoroutineDispatcher> ioDispatcherProvider;

  private CaptionModule_ProvideCaptionModelDownloaderFactory(
      Provider<CaptionModelStore> storeProvider, Provider<OkHttpClient> httpClientProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    this.storeProvider = storeProvider;
    this.httpClientProvider = httpClientProvider;
    this.ioDispatcherProvider = ioDispatcherProvider;
  }

  @Override
  public CaptionModelDownloader get() {
    return provideCaptionModelDownloader(storeProvider.get(), httpClientProvider.get(), ioDispatcherProvider.get());
  }

  public static CaptionModule_ProvideCaptionModelDownloaderFactory create(
      Provider<CaptionModelStore> storeProvider, Provider<OkHttpClient> httpClientProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    return new CaptionModule_ProvideCaptionModelDownloaderFactory(storeProvider, httpClientProvider, ioDispatcherProvider);
  }

  public static CaptionModelDownloader provideCaptionModelDownloader(CaptionModelStore store,
      OkHttpClient httpClient, CoroutineDispatcher ioDispatcher) {
    return Preconditions.checkNotNullFromProvides(CaptionModule.INSTANCE.provideCaptionModelDownloader(store, httpClient, ioDispatcher));
  }
}
