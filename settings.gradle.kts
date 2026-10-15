pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
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

rootProject.name = "BarePlayer"

include(":app")
include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:playback")
include(":core:ui")
include(":feature:library")
include(":feature:player")
include(":feature:playlists")
include(":feature:settings")
include(":baselineprofile")
