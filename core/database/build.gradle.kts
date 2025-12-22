plugins {
    alias(libs.plugins.episodive.android.library)
    alias(libs.plugins.episodive.android.library.jacoco)
    alias(libs.plugins.episodive.android.room)
    alias(libs.plugins.episodive.android.test)
    alias(libs.plugins.episodive.hilt)
}

android {
    namespace = "io.jacob.episodive.core.database"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.model)

    implementation(libs.google.gson)
}