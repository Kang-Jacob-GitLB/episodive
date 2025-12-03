package io.jacob.episodive

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project

internal fun Project.configureTestPackaging(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        packaging.resources {
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
        }
    }
}