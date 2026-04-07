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
            "새 에피소드",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "팔로우한 팟캐스트의 새 에피소드 알림"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun showNewEpisodeNotification(results: List<NewEpisodeResult>) {
        if (results.isEmpty()) return

        val latestResult = results.maxByOrNull { result ->
            result.episodes.maxOf { it.datePublished }
        } ?: return

        val feedTitle = latestResult.episodes.firstOrNull()?.feedTitle ?: "팟캐스트"
        val totalNewEpisodes = results.sumOf { it.episodes.size }

        val title = if (results.size == 1) feedTitle else "새 에피소드"
        val text = if (results.size == 1) {
            "${totalNewEpisodes}개의 새 에피소드"
        } else {
            "${results.size}개 팟캐스트에서 ${totalNewEpisodes}개의 새 에피소드"
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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val CHANNEL_ID = "episodive_new_episodes_channel"
        const val NOTIFICATION_ID = 1002
        const val EXTRA_PODCAST_ID = "podcast_id"
    }
}
