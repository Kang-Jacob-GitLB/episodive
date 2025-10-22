pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Episodive"

include(":app")

include(":core:data")
include(":core:database")
include(":core:datastore")
include(":core:designsystem")
include(":core:domain")
include(":core:model")
include(":core:network")
include(":core:player")
include(":core:testing")

include(":feature:onboarding")
include(":feature:home")
include(":feature:search")
include(":feature:library")
include(":feature:clip")
include(":feature:podcast")
include(":feature:player")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
