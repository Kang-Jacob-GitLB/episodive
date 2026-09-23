package io.jacob.episodive.core.caption.di;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.jacob.episodive.core.caption.asset.CaptionModelStore;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class CaptionModule_ProvideCaptionModelStoreFactory implements Factory<CaptionModelStore> {
  private final Provider<Context> contextProvider;

  private CaptionModule_ProvideCaptionModelStoreFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public CaptionModelStore get() {
    return provideCaptionModelStore(contextProvider.get());
  }

  public static CaptionModule_ProvideCaptionModelStoreFactory create(
      Provider<Context> contextProvider) {
    return new CaptionModule_ProvideCaptionModelStoreFactory(contextProvider);
  }

  public static CaptionModelStore provideCaptionModelStore(Context context) {
    return Preconditions.checkNotNullFromProvides(CaptionModule.INSTANCE.provideCaptionModelStore(context));
  }
}
