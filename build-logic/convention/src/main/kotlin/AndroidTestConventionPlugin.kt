import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import io.jacob.episodive.configureTestPackaging
import io.jacob.episodive.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> {
                    configureTestPackaging(this)
                }
            }
            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> {
                    configureTestPackaging(this)
                }
            }

            tasks.withType<Test> {
                // 테스트 소스는 있지만 발견된 테스트가 없을 경우 빌드 실패하지 않도록 설정
                // Property<Boolean> 타입이므로 set() 메서드를 사용해야 함.
                failOnNoDiscoveredTests.set(false)
            }

            dependencies {
                "testImplementation"(project(":core:testing"))
                "androidTestImplementation"(project(":core:testing"))

                "testImplementation"(libs.findLibrary("androidx.paging.testing").get())

                val bom = libs.findLibrary("androidx.compose.bom").get()
                "androidTestImplementation"(platform(bom))
                "androidTestImplementation"(libs.findLibrary("androidx.compose.ui.test.junit4").get())

                "testImplementation"(platform(bom))
                "testImplementation"(libs.findLibrary("androidx.compose.ui.test.junit4").get())
                "testImplementation"(libs.findLibrary("androidx.compose.ui.test.manifest").get())

                "testImplementation"(libs.findLibrary("androidx.test.core").get())
                "testImplementation"(libs.findLibrary("robolectric").get())
                "testImplementation"(libs.findLibrary("junit").get())
                "androidTestImplementation"(libs.findLibrary("androidx.junit").get())
                "androidTestImplementation"(libs.findLibrary("androidx.espresso.core").get())
                "testImplementation"(libs.findLibrary("kotlinx.coroutines.test").get())
                "androidTestImplementation"(libs.findLibrary("kotlinx.coroutines.test").get())
                "testImplementation"(libs.findLibrary("mockk").get())
                "androidTestImplementation"(libs.findLibrary("mockk.android").get())
                "testImplementation"(libs.findLibrary("turbine").get())
            }
        }
    }
}
