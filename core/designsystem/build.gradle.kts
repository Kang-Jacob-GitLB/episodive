plugins {
    alias(libs.plugins.episodive.android.library)
    alias(libs.plugins.episodive.android.library.compose)
    alias(libs.plugins.episodive.android.library.jacoco)
    alias(libs.plugins.episodive.android.test)
}

android {
    namespace = "io.jacob.episodive.core.designsystem"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    testImplementation(projects.core.testing)
    implementation(projects.core.model)

    //----- Material Icons Extended
    implementation(libs.androidx.compose.material.icons.extended)

    //----- Seeker
    implementation(libs.seeker)

    //----- Coil
    implementation(libs.coil.compose)

    //----- Palette
    implementation(libs.androidx.palette.ktx)

    //----- Paging
    implementation(libs.androidx.paging.compose)
}