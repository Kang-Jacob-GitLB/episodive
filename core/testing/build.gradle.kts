plugins {
    alias(libs.plugins.episodive.android.library)
}

android {
    namespace = "io.jacob.episodive.core.testing"

    packaging.resources {
        excludes += "/META-INF/LICENSE.md"
        excludes += "/META-INF/LICENSE-notice.md"
    }
}

dependencies {
    implementation(projects.core.model)

    //----- Paging
    implementation(libs.androidx.paging.common)

    // ----- Test
    implementation(libs.junit)
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.mockk)
}