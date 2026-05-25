pluginManagement {
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

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Note: Ila 3tak l-LLM mn b3d chi dépendance khassa b Firebase ou autre,
        // l-repositories d Google o MavenCentral dejà 3ndna hna wajdin.
    }
}

rootProject.name = "LeanMassdriss"
include(":app")