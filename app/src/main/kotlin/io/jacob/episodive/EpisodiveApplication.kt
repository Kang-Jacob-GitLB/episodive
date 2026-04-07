package io.jacob.episodive

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.Coil
import coil.ImageLoader
import dagger.hilt.android.HiltAndroidApp
import io.jacob.episodive.sync.EpisodeSyncNotificationHelper
import io.jacob.episodive.sync.EpisodeSyncScheduler
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class EpisodiveApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var episodeSyncScheduler: EpisodeSyncScheduler

    @Inject
    lateinit var episodeSyncNotificationHelper: EpisodeSyncNotificationHelper

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        Coil.setImageLoader(imageLoader)
        Timber.plant(TimberTree(getString(R.string.tag_name)))
        episodeSyncNotificationHelper.createNotificationChannel()
        episodeSyncScheduler.schedule()
    }

    private inner class TimberTree(private val tag: String) : Timber.DebugTree() {
        override fun createStackElementTag(element: StackTraceElement): String {
            return "[${element.fileName}:${element.lineNumber}#${element.methodName}()]"
        }

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            super.log(priority, this.tag, "$tag $message", t)
        }
    }
}
