package io.jacob.episodive.core.caption.engine;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
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
public final class LocaleDeviceLanguageProvider_Factory implements Factory<LocaleDeviceLanguageProvider> {
  @Override
  public LocaleDeviceLanguageProvider get() {
    return newInstance();
  }

  public static LocaleDeviceLanguageProvider_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static LocaleDeviceLanguageProvider newInstance() {
    return new LocaleDeviceLanguageProvider();
  }

  private static final class InstanceHolder {
    static final LocaleDeviceLanguageProvider_Factory INSTANCE = new LocaleDeviceLanguageProvider_Factory();
  }
}
