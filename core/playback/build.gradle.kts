plugins {
    alias(libs.plugins.bareplayer.android.library)
}

android {
    namespace = "com.rajdeepkonwar.bareplayer.core.playback"
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    testImplementation(libs.robolectric)
}
