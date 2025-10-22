plugins {
    alias(libs.plugins.episodive.android.application)
    alias(libs.plugins.episodive.android.application.compose)
    alias(libs.plugins.episodive.android.application.jacoco)
    alias(libs.plugins.episodive.android.hilt)
    alias(libs.plugins.episodive.android.test)
}

android {
    namespace = "io.jacob.episodive"

    defaultConfig {
        applicationId = "io.jacob.episodive"
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.domain)
    implementation(projects.core.designsystem)
    implementation(projects.core.model)

    implementation(projects.feature.onboarding)
    implementation(projects.feature.home)
    implementation(projects.feature.search)
    implementation(projects.feature.library)
    implementation(projects.feature.clip)
    implementation(projects.feature.podcast)
    implementation(projects.feature.player)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.kotlinx.serialization.json)

    //----- Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.animation.compose)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.foundation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}