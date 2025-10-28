package io.jacob.episodive

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

val freeCompilerOptIns = listOf(
    "-opt-in=kotlin.time.ExperimentalTime",
    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
    "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
    "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
)