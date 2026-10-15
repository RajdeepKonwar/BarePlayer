package com.rajdeepkonwar.bareplayer.buildlogic

import com.android.build.api.dsl.Lint
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.lib(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).get()

fun VersionCatalog.int(alias: String): Int = findVersion(alias).get().requiredVersion.toInt()

internal fun Project.configureKotlinJvmTarget() {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

/**
 * Forwards a custom trust store type (e.g. Windows-ROOT behind a TLS-inspecting proxy, set in the
 * user's ~/.gradle/gradle.properties) to test JVMs, which Robolectric uses to fetch Android jars.
 */
internal fun Project.configureTestJvm() {
    val trustStoreType = providers.systemProperty("javax.net.ssl.trustStoreType")
    tasks.withType<Test>().configureEach {
        trustStoreType.orNull?.let { systemProperty("javax.net.ssl.trustStoreType", it) }
    }
}

internal fun Lint.configureLint(project: Project) {
    abortOnError = true
    checkReleaseBuilds = true
    targetSdk = project.libs.int("targetSdk")
    // Version-freshness checks need the network and are handled manually via the catalog.
    disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
}
