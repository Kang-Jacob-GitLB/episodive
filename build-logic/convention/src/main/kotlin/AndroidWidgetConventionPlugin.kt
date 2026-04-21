import com.android.build.gradle.LibraryExtension
import io.jacob.episodive.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Glance 기반 홈 위젯 모듈용 컨벤션 플러그인.
 *
 * AndroidFeatureConventionPlugin 과 달리 Compose UI / Navigation3 / Activity / Paging 등
 * Foreground UI 전용 의존성을 의도적으로 제외한다.
 *
 * 명시 제외(일반 feature 모듈엔 있으나 widget 엔 불필요):
 *   - androidx.navigation3.runtime / navigation3.ui / lifecycle-viewmodel-navigation3
 *   - androidx.activity.compose
 *   - androidx.paging.compose
 *   - seeker (오디오 seek 바)
 *   - androidx.constraintlayout.compose
 *   - androidx.animation.compose
 *   - androidx.lifecycle.runtime.compose
 *   - androidx.lifecycle.viewmodel.compose
 *   - androidx.hilt.navigation.compose
 *   - androidx.foundation.compose
 *   - :core:ui (앱 내부 UI 컴포넌트, Glance 와 호환되지 않음)
 */
class AndroidWidgetConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "episodive.android.library")
            apply(plugin = "episodive.android.library.compose")
            apply(plugin = "episodive.android.library.jacoco")
            apply(plugin = "episodive.android.test")
            apply(plugin = "episodive.hilt")

            extensions.configure<LibraryExtension> {
                testOptions.animationsDisabled = true
            }

            dependencies {
                "implementation"(project(":core:common"))
                "implementation"(project(":core:domain"))
                "implementation"(project(":core:designsystem"))
                "implementation"(project(":core:model"))
                "implementation"(project(":core:data"))
                "implementation"(project(":core:testing"))

                "implementation"(libs.findLibrary("androidx.core.ktx").get())
                "implementation"(libs.findLibrary("androidx.tracing.ktx").get())
                "implementation"(libs.findLibrary("androidx.glance.appwidget").get())
                "implementation"(libs.findLibrary("androidx.glance.material3").get())
                "implementation"(libs.findLibrary("coil").get())
            }
        }
    }
}
