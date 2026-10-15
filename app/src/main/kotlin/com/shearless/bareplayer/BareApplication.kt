package com.shearless.bareplayer

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.shearless.bareplayer.core.playback.PlaybackDependencies
import com.shearless.bareplayer.core.ui.art.BareImageLoader

class BareApplication : Application(), SingletonImageLoader.Factory, PlaybackDependencies.Provider {
    val container: AppContainer by lazy { AppContainer(this) }

    override val playbackDependencies: PlaybackDependencies get() = container

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(container.activityTracker)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = BareImageLoader.create(context)
}
