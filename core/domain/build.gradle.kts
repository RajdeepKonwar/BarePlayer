plugins {
    alias(libs.plugins.bareplayer.jvm.library)
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
}
