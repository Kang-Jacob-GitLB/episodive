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

    implementation(libs.inject)

    implementation(libs.androidx.paging.common)

    // ----- Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)

    testImplementation(projects.core.testing)
}