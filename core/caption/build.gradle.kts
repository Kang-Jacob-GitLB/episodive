plugins {
    alias(libs.plugins.episodive.android.library)
    alias(libs.plugins.episodive.android.library.jacoco)
    alias(libs.plugins.episodive.android.test)
    alias(libs.plugins.episodive.hilt)
}

android {
    namespace = "io.jacob.episodive.core.caption"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.model)
    implementation(projects.core.domain)
    implementation(projects.core.player)

    //----- sherpa-onnx (온디바이스 STT)
    implementation(libs.k2fsa.sherpa.onnx)

    //----- ML Kit (온디바이스 번역)
    implementation(libs.google.mlkit.translate)
    implementation(libs.kotlinx.coroutines.play.services)

    //----- 모델 다운로드용 OkHttp (core:network 클라이언트는 Podcast Index 인증 헤더가 실려 재사용 불가)
    implementation(libs.squareup.okhttp3.okhttp)

    //----- Test
    testImplementation(libs.squareup.okhttp3.mockwebserver)
}
