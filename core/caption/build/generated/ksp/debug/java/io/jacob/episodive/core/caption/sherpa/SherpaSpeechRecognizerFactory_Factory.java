package io.jacob.episodive.core.caption.sherpa;

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
public final class SherpaSpeechRecognizerFactory_Factory implements Factory<SherpaSpeechRecognizerFactory> {
  @Override
  public SherpaSpeechRecognizerFactory get() {
    return newInstance();
  }

  public static SherpaSpeechRecognizerFactory_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SherpaSpeechRecognizerFactory newInstance() {
    return new SherpaSpeechRecognizerFactory();
  }

  private static final class InstanceHolder {
    static final SherpaSpeechRecognizerFactory_Factory INSTANCE = new SherpaSpeechRecognizerFactory_Factory();
  }
}
