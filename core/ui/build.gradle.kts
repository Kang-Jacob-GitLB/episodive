plugins {
    alias(libs.plugins.episodive.android.library)
    alias(libs.plugins.episodive.android.library.compose)
    alias(libs.plugins.episodive.android.library.jacoco)
    alias(libs.plugins.episodive.android.test)
}

android {
    namespace = "io.jacob.episodive.core.ui"
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.testing)
    implementation(projects.core.model)
}