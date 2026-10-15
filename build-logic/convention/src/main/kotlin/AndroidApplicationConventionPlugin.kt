import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.rajdeepkonwar.bareplayer.buildlogic.VerifyNoNetworkPermissionTask
import com.rajdeepkonwar.bareplayer.buildlogic.configureKotlinJvmTarget
import com.rajdeepkonwar.bareplayer.buildlogic.configureLint
import com.rajdeepkonwar.bareplayer.buildlogic.int
import com.rajdeepkonwar.bareplayer.buildlogic.libs
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")

        extensions.configure<ApplicationExtension> {
            compileSdk = libs.int("compileSdk")
            defaultConfig {
                minSdk = libs.int("minSdk")
                targetSdk = libs.int("targetSdk")
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            lint { configureLint(target) }
        }
        configureKotlinJvmTarget()

        val verifyAll = tasks.register("verifyNoInternetPermission") {
            group = "verification"
            description = "Fails if any variant's merged manifest requests a network permission."
        }
        extensions.configure<ApplicationAndroidComponentsExtension> {
            onVariants { variant ->
                val name = variant.name.replaceFirstChar { it.uppercase() }
                val verify = tasks.register<VerifyNoNetworkPermissionTask>("verify${name}NoInternetPermission") {
                    mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
                    report.set(layout.buildDirectory.file("reports/network-permissions/${variant.name}.txt"))
                }
                verifyAll.configure { dependsOn(verify) }
            }
        }
        tasks.named("check").configure { dependsOn(verifyAll) }
    }
}
