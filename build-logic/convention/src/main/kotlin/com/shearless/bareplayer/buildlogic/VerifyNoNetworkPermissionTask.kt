package com.shearless.bareplayer.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails the build if the merged manifest requests any network permission. BarePlayer must stay
 * fully offline, so a transitive dependency silently adding INTERNET is treated as a build error.
 */
@CacheableTask
abstract class VerifyNoNetworkPermissionTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val manifest = mergedManifest.get().asFile.readText()
        val requested = PERMISSION_REGEX.findAll(manifest).map { it.groupValues[1] }.toSet()
        val violations = requested intersect FORBIDDEN
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Merged manifest requests forbidden network permission(s): " +
                    violations.sorted().joinToString() +
                    ". Remove the offending dependency or strip it with tools:node=\"remove\".",
            )
        }
        report.get().asFile.writeText("Requested permissions:\n" + requested.sorted().joinToString("\n"))
    }

    private companion object {
        val PERMISSION_REGEX = Regex("""<uses-permission(?:-sdk-23)?\b[^>]*?android:name="([^"]+)"""")
        val FORBIDDEN = setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.CHANGE_NETWORK_STATE",
        )
    }
}
