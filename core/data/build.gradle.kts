plugins {
    alias(libs.plugins.episodive.android.library)
    alias(libs.plugins.episodive.android.library.jacoco)
    alias(libs.plugins.episodive.android.test)
    alias(libs.plugins.episodive.hilt)
}

android {
    namespace = "io.jacob.episodive.core.data"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.network)
    implementation(projects.core.player)

    //----- Media3
    implementation(libs.androidx.media3.common)

    //----- Coil
    implementation(libs.coil)

    //----- Palette
    implementation(libs.androidx.palette.ktx)

    //----- Paging
    implementation(libs.androidx.paging.runtime)

    //----- Room
    implementation(libs.androidx.room.runtime)
}