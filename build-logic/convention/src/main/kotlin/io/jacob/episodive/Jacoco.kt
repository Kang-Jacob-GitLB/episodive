package io.jacob.episodive

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.api.variant.SourceDirectories
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.util.Locale

private val coverageExclusions = listOf(
    // Android
    "**/R.class",
    "**/R\$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    // Hilt generated classes
    "**/*_Hilt*.class",
    "**/Hilt_*.class",
    "**/hilt_aggregated_deps/**",
    "**/*_HiltModules*.class",
    "**/*_Provide*.class",
    "**/*Module_Provide*.class",
    "**/*_Factory.class",
    "**/*_MembersInjector.class",
    // Dagger generated classes
    "**/*_Generated.class",
    "**/Dagger*.class",
    "**/*Component\$Builder.class",
    "**/*Component\$*.class",
    "**/*Subcomponent*.class",

    // Model classes
    "**/model/**/*.class",
    // DI classes
    "**/di/**/*.class",
    // Repository interfaces
    "**/repository/*Repository.class",
    // DataSource interfaces
    "**/datasource/*DataSource.class",
    // Network API classes
    "**/network/api/**/*Api.class",
    // Database generated classes
    "**/database/*AutoMigration*Impl.class",
    "**/database/**/*Dao_Impl.class",
    "**/database/**/*Dao_Impl\$*.class",
    "**/database/**/*Database_Impl.class",
    "**/database/**/*Database_Impl\$*.class",
    "**/database/migration/**/*.class",
    // Download classes (Android system dependencies)
    "**/download/**/*.class",
    // Compose Screen/Bar composables (UI code tested via integration tests)
    "**/*ScreenKt*.class",
    "**/*BarKt*.class",
    // Navigation classes (Compose Navigation glue code)
    "**/navigation/**/*.class",
    // Route classes (serializable route data objects)
    "**/*Route.class",
    "**/*Route\$*.class",
    "**/*BaseRoute.class",
    "**/*BaseRoute\$*.class",
    // Android framework classes (tested via instrumented tests)
    "**/*Activity.class",
    "**/*Activity\$*.class",
    "**/*Service.class",
    "**/*Service\$*.class",
    "**/*Application.class",
    "**/*Application\$*.class",
    // Media service implementation details (CustomCommand enum)
    "**/CustomCommand.class",
    "**/CustomCommand\$*.class",
    // Top-level Compose app shell (not unit-testable)
    "**/*AppKt*.class",
    "**/*AppState*.class",
    // Compose design system components (UI tested via screenshot/integration tests)
    "**/designsystem/**/*.class",
)

private fun String.capitalize() = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
}

/**
 * Creates a new task that generates a combined coverage report with data from local and
 * instrumented tests.
 *
 * `create{variant}CombinedCoverageReport`
 *
 * Note that coverage data must exist before running the task. This allows us to run device
 * tests on CI using a different Github Action or an external device farm.
 */
internal fun Project.configureJacoco(
    androidComponentsExtension: AndroidComponentsExtension<*, *, *>,
) {
    configure<JacocoPluginExtension> {
        toolVersion = libs.findVersion("jacoco").get().toString()
    }

    androidComponentsExtension.onVariants { variant ->
        val myObjFactory = project.objects
        val buildDir = layout.buildDirectory.get().asFile
        val allJars: ListProperty<RegularFile> = myObjFactory.listProperty(RegularFile::class.java)
        val allDirectories: ListProperty<Directory> =
            myObjFactory.listProperty(Directory::class.java)
        val reportTask =
            tasks.register(
                "create${variant.name.capitalize()}CombinedCoverageReport",
                JacocoReport::class,
            ) {
                dependsOn("test${variant.name.capitalize()}UnitTest")

                classDirectories.setFrom(
                    allJars.map { jars ->
                        jars.map { jar ->
                            project.zipTree(jar.asFile).matching {
                                include("io/jacob/episodive/**")
                                exclude(coverageExclusions)
                            }
                        }
                    },
                    allDirectories.map { dirs ->
                        dirs.map { dir ->
                            myObjFactory.fileTree().setDir(dir).exclude(coverageExclusions)
                        }
                    },
                )
                reports {
                    xml.required = true
                    html.required = true
                }

                fun SourceDirectories.Flat?.toFilePaths(): Provider<List<String>> = this
                    ?.all
                    ?.map { directories -> directories.map { it.asFile.path } }
                    ?: provider { emptyList() }
                sourceDirectories.setFrom(
                    files(
                        variant.sources.java.toFilePaths(),
                        variant.sources.kotlin.toFilePaths()
                    ),
                )

                executionData.setFrom(
                    project.fileTree("$buildDir/outputs/unit_test_code_coverage/${variant.name}UnitTest")
                        .matching { include("**/*.exec") },

                    project.fileTree("$buildDir/outputs/code_coverage/${variant.name}AndroidTest")
                        .matching { include("**/*.ec") },
                )
            }


        variant.artifacts.forScope(ScopedArtifacts.Scope.PROJECT)
            .use(reportTask)
            .toGet(
                ScopedArtifact.CLASSES,
                { _ -> allJars },
                { _ -> allDirectories },
            )
    }

    tasks.withType<Test>().configureEach {
        configure<JacocoTaskExtension> {
            // Required for JaCoCo + Robolectric
            // https://github.com/robolectric/robolectric/issues/2230
            isIncludeNoLocationClasses = true

            // Required for JDK 11 with the above
            // https://github.com/gradle/gradle/issues/5184#issuecomment-391982009
            excludes = listOf("jdk.internal.*")
        }
    }
}
