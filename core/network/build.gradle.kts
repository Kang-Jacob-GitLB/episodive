import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

plugins {
    alias(libs.plugins.episodive.android.library)
    alias(libs.plugins.episodive.android.library.jacoco)
    alias(libs.plugins.episodive.android.test)
    alias(libs.plugins.episodive.hilt)
}

android {
    namespace = "io.jacob.episodive.core.network"

    buildTypes {
        all {
            buildConfigField("String", "API_KEY", "\"${localProperties["podcastIndex.apiKey"]}\"")
            buildConfigField("String", "SECRET_KEY", "\"${localProperties["podcastIndex.secretKey"]}\"")
        }
    }
    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.model)

    //----- Retrofit
    implementation(libs.squareup.retrofit2.retrofit)
    implementation(libs.squareup.retrofit2.converter.gson)
    implementation(libs.squareup.okhttp3.logging.interceptor)
    implementation(libs.google.gson)

    //----- Test
    testImplementation(libs.squareup.okhttp3.mockwebserver)
}