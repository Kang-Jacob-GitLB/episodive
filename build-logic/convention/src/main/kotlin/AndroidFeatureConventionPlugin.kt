import com.android.build.gradle.LibraryExtension
import io.jacob.episodive.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "episodive.android.library")
            apply(plugin = "episodive.android.library.compose")
            apply(plugin = "episodive.android.library.jacoco")
            apply(plugin = "episodive.android.hilt")
            apply(plugin = "episodive.android.test")

            extensions.configure<LibraryExtension> {
                testOptions.animationsDisabled = true
            }

            dependencies {
                "implementation"(project(":core:domain"))
                "implementation"(project(":core:designsystem"))
                "implementation"(project(":core:testing"))
                "implementation"(project(":core:model"))

                "implementation"(libs.findLibrary("androidx.activity.compose").get())
                "implementation"(libs.findLibrary("androidx.animation.compose").get())
                "implementation"(libs.findLibrary("androidx.constraintlayout.compose").get())
                "implementation"(libs.findLibrary("androidx.foundation.compose").get())
                "implementation"(libs.findLibrary("androidx.hilt.navigation.compose").get())
                "implementation"(libs.findLibrary("androidx.lifecycle.runtime.compose").get())
                "implementation"(libs.findLibrary("androidx.lifecycle.viewmodel.compose").get())
                "implementation"(libs.findLibrary("androidx.navigation.compose").get())
                "implementation"(libs.findLibrary("androidx.paging.compose").get())
            }
        }
    }
}
