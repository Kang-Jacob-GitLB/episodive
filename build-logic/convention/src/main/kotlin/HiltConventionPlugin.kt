import io.jacob.episodive.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.google.devtools.ksp")

            dependencies {
                "ksp"(libs.findLibrary("google.hilt.compiler").get())
                "kspTest"(libs.findLibrary("google.hilt.compiler").get())
            }

            pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                dependencies {
                    "implementation"(libs.findLibrary("google.hilt.core").get())
                }
            }

            pluginManager.withPlugin("com.android.base") {
                apply(plugin = "dagger.hilt.android.plugin")
                dependencies {
                    "implementation"(libs.findLibrary("google.hilt.android").get())

                    "testImplementation"(libs.findLibrary("google.hilt.android.testing").get())
                    "androidTestImplementation"(
                        libs.findLibrary("google.hilt.android.testing").get()
                    )
                    "kspAndroidTest"(libs.findLibrary("google.hilt.compiler").get())
                }
            }
        }
    }
}
