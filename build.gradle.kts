// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.stability.analyzer) apply false
}

tasks.register<Exec>("generateCoverageReport") {
    group = "verification"
    description = "Generate a markdown coverage report from Jacoco XML reports (outputs to docs/)"

    dependsOn(
        subprojects.mapNotNull { subproject ->
            subproject.tasks.findByName("createDebugCombinedCoverageReport")
        }
    )

    workingDir = projectDir
    commandLine("python3", "scripts/analyze_coverage.py")

    val reportPath = layout.projectDirectory.file("docs/COVERAGE_REPORT.md").asFile.absolutePath

    doLast {
        println("\n✅ Coverage report generated successfully!")
        println("📄 Location: $reportPath")
    }
}