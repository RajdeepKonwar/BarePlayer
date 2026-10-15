plugins {
    alias(libs.plugins.bareplayer.android.library)
    alias(libs.plugins.bareplayer.android.compose)
}

android {
    namespace = "com.shearless.bareplayer.core.ui"
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.coil.compose)
}
