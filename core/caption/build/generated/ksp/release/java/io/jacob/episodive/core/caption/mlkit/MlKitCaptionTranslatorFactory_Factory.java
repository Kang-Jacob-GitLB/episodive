package io.jacob.episodive.core.caption.mlkit;

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
public final class MlKitCaptionTranslatorFactory_Factory implements Factory<MlKitCaptionTranslatorFactory> {
  @Override
  public MlKitCaptionTranslatorFactory get() {
    return newInstance();
  }

  public static MlKitCaptionTranslatorFactory_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static MlKitCaptionTranslatorFactory newInstance() {
    return new MlKitCaptionTranslatorFactory();
  }

  private static final class InstanceHolder {
    static final MlKitCaptionTranslatorFactory_Factory INSTANCE = new MlKitCaptionTranslatorFactory_Factory();
  }
}
