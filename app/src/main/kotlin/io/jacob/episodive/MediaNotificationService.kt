package io.jacob.episodive

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
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
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.GetNowPlayingUseCase
import io.jacob.episodive.core.model.Episode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MediaNotificationService : MediaSessionService() {

    @Inject
    @Player(EpisodivePlayers.Main)
    lateinit var playerRepository: PlayerRepository

    @Inject
    lateinit var getNowPlayingUseCase: GetNowPlayingUseCase

    @Inject
    lateinit var toggleLikedEpisodeUseCase: ToggleLikedEpisodeUseCase

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var nowPlaying: Episode
    private var isLiked = false

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

        serviceScope.launch {
            getNowPlayingUseCase().collectLatest { episode ->
                episode?.let { nowPlaying = it }
                updateCustomLayout(episode?.isLiked ?: false)
            }
        }
    }

    private fun updateCustomLayout(isLiked: Boolean) {
        val customLayout = getCustomLayout(isLiked)
        mediaSession?.setCustomLayout(customLayout)
    }

    private fun getCustomLayout(): ImmutableList<CommandButton> {
        return getCustomLayout(isLiked)
    }

    private fun getCustomLayout(isLiked: Boolean): ImmutableList<CommandButton> {
        val likeIcon = if (isLiked) {
            CommandButton.ICON_HEART_FILLED
        } else {
            CommandButton.ICON_HEART_UNFILLED
        }

        return ImmutableList.of(
            CommandButton.Builder(CommandButton.ICON_SKIP_BACK_15)
                .setDisplayName("Seek Backward")
                .setSessionCommand(CustomCommand.SEEK_BACKWARD.sessionCommand)
                .build(),
            CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_30)
                .setDisplayName("Seek Forward")
                .setSessionCommand(CustomCommand.SEEK_FORWARD.sessionCommand)
                .build(),
            CommandButton.Builder(likeIcon)
                .setDisplayName("Toggle Like")
                .setSessionCommand(CustomCommand.TOGGLE_LIKE.sessionCommand)
                .build()
        )
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_playback),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_playback_description)
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
                .addSessionCommands(CustomCommand.sessionCommands)
                .build()

            // Remove previous/next track commands to hide those buttons
            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
                .buildUpon()
                .remove(androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS)
                .remove(androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT)
                .remove(androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .remove(androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(playerCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (CustomCommand.fromAction(customCommand.customAction)) {
                CustomCommand.SEEK_BACKWARD -> playerRepository.seekBackward()
                CustomCommand.SEEK_FORWARD -> playerRepository.seekForward()
                CustomCommand.TOGGLE_LIKE -> toggleLikeEpisode()
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
        serviceScope.cancel()
        mediaSession?.release()
        mediaSession = null

        super.onDestroy()
    }

    private fun toggleLikeEpisode() = serviceScope.launch {
        toggleLikedEpisodeUseCase(nowPlaying)
    }
}

private enum class CustomCommand(
    val action: String,
    val icon: Int,
    val displayName: String,
) {
    SEEK_BACKWARD(
        action = "SEEK_BACKWARD_15",
        icon = CommandButton.ICON_SKIP_BACK_15,
        displayName = "Seek Backward"
    ),
    SEEK_FORWARD(
        action = "SEEK_FORWARD_30",
        icon = CommandButton.ICON_SKIP_FORWARD_30,
        displayName = "Seek Forward"
    ),
    TOGGLE_LIKE(
        action = "TOGGLE_LIKE",
        icon = CommandButton.ICON_HEART_UNFILLED,  // TODO: Dynamic icon based on isLiked state
        displayName = "Toggle Like"
    )
    ;

    val sessionCommand: SessionCommand by lazy {
        SessionCommand(action, Bundle.EMPTY)
    }

    val commandButton: CommandButton by lazy {
        CommandButton.Builder(icon)
            .setDisplayName(displayName)
            .setSessionCommand(sessionCommand)
            .build()
    }

    companion object {
        val sessionCommands: List<SessionCommand> by lazy {
            entries.map { it.sessionCommand }
        }

        val commandButtons: List<CommandButton> by lazy {
            entries.map { it.commandButton }
        }

        fun fromAction(action: String): CustomCommand? {
            return entries.find { it.action == action }
        }
    }
}