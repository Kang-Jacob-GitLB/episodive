package io.jacob.episodive.core.caption.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class CaptionModule_ProvideCaptionDispatcherFactory implements Factory<CoroutineDispatcher> {
  @Override
  public CoroutineDispatcher get() {
    return provideCaptionDispatcher();
  }

  public static CaptionModule_ProvideCaptionDispatcherFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static CoroutineDispatcher provideCaptionDispatcher() {
    return Preconditions.checkNotNullFromProvides(CaptionModule.INSTANCE.provideCaptionDispatcher());
  }

  private static final class InstanceHolder {
    static final CaptionModule_ProvideCaptionDispatcherFactory INSTANCE = new CaptionModule_ProvideCaptionDispatcherFactory();
  }
}
