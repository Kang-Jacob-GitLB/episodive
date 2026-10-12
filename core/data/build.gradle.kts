import org.gradle.process.CommandLineArgumentProvider
import java.io.File

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
    implementation(projects.core.caption)
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

// KoreanStationCatalogTest 가 읽는 cleartext 허용 목록. 입력은 파일 내용이어야 한다 — org.gradle.caching 때문에
// XML 만 바뀐 실행이 캐시로 건너뛰어진다. 경로를 systemProperty 로 직접 넘기면 절대경로가 캐시 키에 섞여
// RELATIVE 가 무력해지므로, 파일은 입력으로만 선언하고 경로는 인자 제공자로 넘긴다.
class NetworkSecurityConfigArgs(
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val file: File,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> =
        listOf("-Depisodive.networkSecurityConfig=${file.absolutePath}")
}

tasks.withType<Test>().configureEach {
    jvmArgumentProviders.add(
        NetworkSecurityConfigArgs(file("../../app/src/main/res/xml/network_security_config.xml"))
    )
}
