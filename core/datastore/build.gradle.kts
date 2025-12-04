plugins {
    alias(libs.plugins.episodive.android.library)
    alias(libs.plugins.episodive.android.library.jacoco)
    alias(libs.plugins.episodive.android.test)
    alias(libs.plugins.episodive.hilt)
}

android {
    namespace = "io.jacob.episodive.core.datastore"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.model)

    //----- DataStore
    implementation(libs.androidx.datastore.preferences)
}