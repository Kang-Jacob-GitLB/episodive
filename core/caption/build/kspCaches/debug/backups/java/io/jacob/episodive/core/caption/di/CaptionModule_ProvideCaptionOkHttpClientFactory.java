package io.jacob.episodive.core.caption.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("io.jacob.episodive.core.caption.di.CaptionOkHttpClient")
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
public final class CaptionModule_ProvideCaptionOkHttpClientFactory implements Factory<OkHttpClient> {
  @Override
  public OkHttpClient get() {
    return provideCaptionOkHttpClient();
  }

  public static CaptionModule_ProvideCaptionOkHttpClientFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static OkHttpClient provideCaptionOkHttpClient() {
    return Preconditions.checkNotNullFromProvides(CaptionModule.INSTANCE.provideCaptionOkHttpClient());
  }

  private static final class InstanceHolder {
    static final CaptionModule_ProvideCaptionOkHttpClientFactory INSTANCE = new CaptionModule_ProvideCaptionOkHttpClientFactory();
  }
}
