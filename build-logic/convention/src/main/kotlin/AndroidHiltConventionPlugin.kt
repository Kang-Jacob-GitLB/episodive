import io.jacob.episodive.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "dagger.hilt.android.plugin")
            apply(plugin = "com.google.devtools.ksp")

            dependencies {
                "implementation"(libs.findLibrary("google.hilt.android").get())
                "ksp"(libs.findLibrary("google.hilt.compiler").get())
            }

            pluginManager.withPlugin("com.android.application") {
                dependencies {
                    "testImplementation"(libs.findLibrary("google.hilt.testing").get())
                    "kspTest"(libs.findLibrary("google.hilt.compiler").get())
                    "androidTestImplementation"(libs.findLibrary("google.hilt.testing").get())
                    "kspAndroidTest"(libs.findLibrary("google.hilt.compiler").get())
                }
            }
        }
    }
}
