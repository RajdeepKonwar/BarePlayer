package com.shearless.bareplayer.baselineprofile

import android.os.Build
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

internal const val PACKAGE_NAME = "com.shearless.bareplayer"

/** Pre-grants audio access so journeys land on the library instead of the permission screen. */
internal fun MacrobenchmarkScope.grantAudioPermission() {
    val permission = if (Build.VERSION.SDK_INT >= 33) {
        "android.permission.READ_MEDIA_AUDIO"
    } else {
        "android.permission.READ_EXTERNAL_STORAGE"
    }
    device.executeShellCommand("pm grant $packageName $permission")
    if (Build.VERSION.SDK_INT >= 33) {
        device.executeShellCommand("pm grant $packageName android.permission.POST_NOTIFICATIONS")
    }
}

/** Flings the songs list down and back up; needs music on the device to be meaningful. */
internal fun MacrobenchmarkScope.scrollSongs() {
    val list = device.wait(Until.findObject(By.res("songs_list")), 5_000) ?: return
    list.setGestureMargin(device.displayWidth / 5)
    repeat(3) { list.fling(Direction.DOWN) }
    repeat(3) { list.fling(Direction.UP) }
    device.waitForIdle()
}
