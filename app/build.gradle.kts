plugins {
    alias(libs.plugins.episodive.android.application)
    alias(libs.plugins.episodive.android.application.compose)
    alias(libs.plugins.episodive.android.application.jacoco)
    alias(libs.plugins.episodive.android.test)
    alias(libs.plugins.episodive.hilt)
}

android {
    namespace = "io.jacob.episodive"

    defaultConfig {
        applicationId = "io.jacob.episodive"
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 라이브 자막의 sherpa-onnx aar 가 네 ABI 의 onnxruntime 을 모두 싣고 있어(약 48MB)
    // 하나의 APK 에 담으면 기기와 무관한 네이티브 라이브러리가 대부분을 차지한다.
    // ABI 별로 APK 를 나눠 각자 자기 라이브러리만 싣게 하고, 릴리즈도 ABI 별로 올린다
    // (.github/workflows/publish.yml). universal APK 는 만들지 않는다.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
            isUniversalApk = false
        }
    }

    packaging {
        jniLibs {
            // sherpa-onnx aar 는 C/C++ API 용 .so 도 함께 싣지만, 코틀린 API 가 여는
            // libsherpa-onnx-jni.so 는 libonnxruntime.so 만 NEEDED 로 가진다(readelf 확인).
            // 아무도 열지 않는 두 파일이 ABI 마다 약 4.9MB 를 차지하므로 뺀다.
            excludes += listOf(
                "**/libsherpa-onnx-c-api.so",
                "**/libsherpa-onnx-cxx-api.so",
            )
        }
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
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.domain)
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
    implementation(projects.core.ui)

    implementation(projects.feature.onboarding)
    implementation(projects.feature.home)
    implementation(projects.feature.search)
    implementation(projects.feature.library)
    implementation(projects.feature.clip)
    implementation(projects.feature.channel)
    implementation(projects.feature.podcast)
    implementation(projects.feature.player)
    implementation(projects.feature.widget)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.tracing.ktx)
    implementation(libs.kotlinx.serialization.json)

    //----- WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    testImplementation(libs.androidx.work.testing)

    //----- Media3 Session
    implementation(libs.androidx.media3.session)

    //----- Coil
    implementation(libs.coil.compose)

    //----- Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.animation.compose)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.foundation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    //----- Coil
    testImplementation(libs.coil.test)

    //----- Leak Canary
    debugImplementation(libs.squareup.leakcanary)
}