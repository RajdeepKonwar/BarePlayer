# BarePlayer

An offline, ad-free local music player for Android. No internet permission, no analytics, no tracking.

## Build and run

Requirements: JDK 17+ (JDK 21 recommended) and the Android SDK with platform 37 installed. Point
`ANDROID_HOME` at the SDK (or create `local.properties` with `sdk.dir=...`).

```powershell
./gradlew assembleDebug                 # build the debug APK
./gradlew lint test verifyNoInternetPermission   # the per-milestone quality gate
adb install app/build/outputs/apk/debug/app-debug.apk
```

`verifyNoInternetPermission` fails the build if the merged manifest of any variant requests
`INTERNET`, `ACCESS_NETWORK_STATE`, or other network permissions. It also runs as part of `check`.

## Architecture

See the module overview below; the full architecture section is completed at the end of the MVP.

| Module | Type | Responsibility |
| --- | --- | --- |
| `:core:model` | Kotlin/JVM | Immutable domain models |
| `:core:domain` | Kotlin/JVM | Repository interfaces, playback contract, pure queue/search logic |
| `:core:data` | Android library | MediaStore scanning, Room index, DataStore preferences |
| `:core:playback` | Android library | Media3 `MediaLibraryService`, ExoPlayer, `MediaController` bridge |
| `:core:ui` | Android library | Material 3 theme, icons, shared Compose components, artwork loading |
| `:feature:*` | Android library | Screens and ViewModels |
| `:app` | Application | Manual DI container, navigation, permissions |
| `build-logic` | Included build | Gradle convention plugins |
