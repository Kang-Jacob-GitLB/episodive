plugins {
    alias(libs.plugins.episodive.android.library)
    alias(libs.plugins.episodive.android.library.jacoco)
}

android {
    namespace = "io.jacob.episodive.core.domain"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.model)
    testImplementation(projects.core.testing)

    implementation(libs.inject)

    //----- Paging
    implementation(libs.androidx.paging.common)
    testImplementation(libs.androidx.paging.testing)

    //----- Media3
    implementation(libs.androidx.media3.common)

    //----- Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
}