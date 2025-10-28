package io.jacob.episodive

import android.app.Application
import coil.Coil
import coil.ImageLoader
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class EpisodiveApplication : Application() {
    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreate() {
        super.onCreate()
        Coil.setImageLoader(imageLoader)
        Timber.plant(TimberTree(getString(R.string.tag_name)))
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