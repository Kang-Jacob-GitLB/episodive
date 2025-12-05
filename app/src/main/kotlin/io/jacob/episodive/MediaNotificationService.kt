package io.jacob.episodive

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.domain.repository.PlayerRepository
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MediaNotificationService : MediaSessionService() {

    @Inject
    @Player(EpisodivePlayers.Main)
    lateinit var playerRepository: PlayerRepository

    private var mediaSession: MediaSession? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "episodive_playback_channel"
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val player = playerRepository.getPlayer()

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(MediaSessionCallback())
            .setCustomLayout(getCustomLayout())
            .setSessionActivity(createPendingIntent())
            .build()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setNotificationId(NOTIFICATION_ID)
            .build()

        setMediaNotificationProvider(notificationProvider)
    }

    private fun getCustomLayout(): ImmutableList<CommandButton> {
        return ImmutableList.copyOf(CustomCommand.ALL.map { it.button })
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media playback controls"
                setShowBadge(false)
            }

            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .apply {
                    CustomCommand.ALL.forEach { add(it.sessionCommand) }
                }
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (CustomCommand.fromAction(customCommand.customAction)) {
                CustomCommand.SeekBackward -> playerRepository.seekBackward()
                CustomCommand.SeekForward -> playerRepository.seekForward()
                null -> {} // Unknown command
            }
            return Futures.immediateFuture(
                SessionResult(
                    SessionResult.RESULT_SUCCESS,
                    Bundle.EMPTY
                )
            )
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            return Futures.immediateFuture(mediaItems)
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

private sealed interface CustomCommand {
    val action: String
    val sessionCommand: SessionCommand
    val button: CommandButton

    data object SeekBackward : CustomCommand {
        override val action = "SEEK_BACKWARD_15"
        override val sessionCommand = SessionCommand(action, Bundle.EMPTY)
        override val button = CommandButton.Builder(CommandButton.ICON_SKIP_BACK_15)
            .setDisplayName("Seek Backward")
            .setSessionCommand(sessionCommand)
            .build()
    }

    data object SeekForward : CustomCommand {
        override val action = "SEEK_FORWARD_30"
        override val sessionCommand = SessionCommand(action, Bundle.EMPTY)
        override val button = CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_30)
            .setDisplayName("Seek Forward")
            .setSessionCommand(sessionCommand)
            .build()
    }

    companion object {
        val ALL = listOf(SeekBackward, SeekForward)

        fun fromAction(action: String): CustomCommand? {
            return ALL.find { it.action == action }
        }
    }
}