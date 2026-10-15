plugins {
    alias(libs.plugins.bareplayer.android.library)
}

android {
    namespace = "com.shearless.bareplayer.core.billing"
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.billing)
}
