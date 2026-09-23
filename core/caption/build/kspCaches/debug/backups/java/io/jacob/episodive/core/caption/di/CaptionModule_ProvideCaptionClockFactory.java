package io.jacob.episodive.core.caption.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.jacob.episodive.core.caption.engine.CaptionClock;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
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
public final class CaptionModule_ProvideCaptionClockFactory implements Factory<CaptionClock> {
  @Override
  public CaptionClock get() {
    return provideCaptionClock();
  }

  public static CaptionModule_ProvideCaptionClockFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static CaptionClock provideCaptionClock() {
    return Preconditions.checkNotNullFromProvides(CaptionModule.INSTANCE.provideCaptionClock());
  }

  private static final class InstanceHolder {
    static final CaptionModule_ProvideCaptionClockFactory INSTANCE = new CaptionModule_ProvideCaptionClockFactory();
  }
}
