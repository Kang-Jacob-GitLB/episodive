package io.jacob.episodive.core.caption.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.jacob.episodive.core.caption.asset.CaptionModelDownloader;
import io.jacob.episodive.core.caption.asset.CaptionModelManager;
import io.jacob.episodive.core.caption.asset.CaptionModelStore;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineScope;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("io.jacob.episodive.core.common.ApplicationScope")
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
public final class CaptionModule_ProvideCaptionModelManagerFactory implements Factory<CaptionModelManager> {
  private final Provider<CaptionModelStore> storeProvider;

  private final Provider<CaptionModelDownloader> downloaderProvider;

  private final Provider<CoroutineScope> scopeProvider;

  private CaptionModule_ProvideCaptionModelManagerFactory(Provider<CaptionModelStore> storeProvider,
      Provider<CaptionModelDownloader> downloaderProvider, Provider<CoroutineScope> scopeProvider) {
    this.storeProvider = storeProvider;
    this.downloaderProvider = downloaderProvider;
    this.scopeProvider = scopeProvider;
  }

  @Override
  public CaptionModelManager get() {
    return provideCaptionModelManager(storeProvider.get(), downloaderProvider.get(), scopeProvider.get());
  }

  public static CaptionModule_ProvideCaptionModelManagerFactory create(
      Provider<CaptionModelStore> storeProvider,
      Provider<CaptionModelDownloader> downloaderProvider, Provider<CoroutineScope> scopeProvider) {
    return new CaptionModule_ProvideCaptionModelManagerFactory(storeProvider, downloaderProvider, scopeProvider);
  }

  public static CaptionModelManager provideCaptionModelManager(CaptionModelStore store,
      CaptionModelDownloader downloader, CoroutineScope scope) {
    return Preconditions.checkNotNullFromProvides(CaptionModule.INSTANCE.provideCaptionModelManager(store, downloader, scope));
  }
}
