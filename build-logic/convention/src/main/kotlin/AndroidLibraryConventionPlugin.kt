import com.android.build.api.dsl.LibraryExtension
import com.shearless.bareplayer.buildlogic.configureKotlinJvmTarget
import com.shearless.bareplayer.buildlogic.configureLint
import com.shearless.bareplayer.buildlogic.configureTestJvm
import com.shearless.bareplayer.buildlogic.int
import com.shearless.bareplayer.buildlogic.lib
import com.shearless.bareplayer.buildlogic.libs
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")

        extensions.configure<LibraryExtension> {
            compileSdk = libs.int("compileSdk")
            defaultConfig {
                minSdk = libs.int("minSdk")
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            testOptions {
                targetSdk = libs.int("targetSdk")
                unitTests.isIncludeAndroidResources = true
            }
            lint { configureLint(target) }
        }
        configureKotlinJvmTarget()
        configureTestJvm()

        dependencies {
            add("testImplementation", libs.lib("junit"))
            add("testImplementation", libs.lib("kotlinx-coroutines-test"))
        }
    }
}
