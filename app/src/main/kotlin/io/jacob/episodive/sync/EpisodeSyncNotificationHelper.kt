package io.jacob.episodive.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.jacob.episodive.MainActivity
import io.jacob.episodive.R
import io.jacob.episodive.core.domain.usecase.episode.NewEpisodeResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeSyncNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_new_episodes),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_new_episodes_description)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    suspend fun showNewEpisodeNotification(results: List<NewEpisodeResult>) {
        if (results.isEmpty()) return

        val latestResult = results.maxByOrNull { result ->
            result.episodes.maxOf { it.datePublished }
        } ?: return

        val feedTitle = latestResult.episodes.firstOrNull()?.feedTitle
            ?: context.getString(R.string.notification_podcast_fallback)
        val totalNewEpisodes = results.sumOf { it.episodes.size }

        val title = if (results.size == 1) {
            feedTitle
        } else {
            context.getString(R.string.notification_new_episodes_title)
        }
        val text = if (results.size == 1) {
            context.getString(R.string.notification_new_episodes_single, totalNewEpisodes)
        } else {
            context.getString(R.string.notification_new_episodes_multiple, totalNewEpisodes, results.size)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PODCAST_ID, latestResult.feedId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val latestEpisode = results.flatMap { it.episodes }
            .maxByOrNull { it.datePublished }
        val largeIcon = latestEpisode?.image?.let { loadBitmap(it) }
            ?: latestEpisode?.feedImage?.let { loadBitmap(it) }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.media3_notification_small_icon)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .apply { if (largeIcon != null) setLargeIcon(largeIcon) }
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }


    private suspend fun loadBitmap(url: String): android.graphics.Bitmap? {
        return try {
            val request = coil.request.ImageRequest.Builder(context)
                .data(url)
                .size(256)
                .allowHardware(false)
                .build()
            val result = coil.Coil.imageLoader(context).execute(request)
            (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val CHANNEL_ID = "episodive_new_episodes_channel"
        const val NOTIFICATION_ID = 1002
        const val EXTRA_PODCAST_ID = "podcast_id"
    }
}
