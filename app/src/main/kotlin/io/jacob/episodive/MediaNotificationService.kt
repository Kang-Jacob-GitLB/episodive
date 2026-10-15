package io.jacob.episodive

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionError
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import coil.Coil
import coil.request.ImageRequest
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.data.widget.WidgetUpdater
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.usecase.episode.GetMostRecentPlayedEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.GetNowPlayingUseCase
import io.jacob.episodive.core.domain.usecase.player.WidgetPlayPauseUseCase
import io.jacob.episodive.core.domain.usecase.station.ObserveOnAirProgramUseCase
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.OnAirProgram
import io.jacob.episodive.core.model.Station
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

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

    @Inject
    lateinit var getMostRecentPlayedEpisodeUseCase: GetMostRecentPlayedEpisodeUseCase

    @Inject
    lateinit var widgetPlayPauseUseCase: WidgetPlayPauseUseCase

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    @Inject
    lateinit var observeOnAirProgramUseCase: ObserveOnAirProgramUseCase

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 스테이션을 먼저 틀면 에피소드가 한 번도 없을 수 있다. 두 수집기는 각자 필드만 갱신하고,
    // 레이아웃은 그 필드를 읽어 정한다 — combine 으로 묶으면 한쪽이 늦을 때 어긋난 쌍을 본다.
    private var nowPlaying: Episode? = null
    private var currentStation: Station? = null

    companion object {
        private const val NOTIFICATION_ID = 1001

        // 알림 큰 아이콘용 프로그램 이미지. 원본(최대 1280px)을 그대로 세션에 싣지 않는다.
        private const val ARTWORK_SIZE_PX = 512
        private const val ARTWORK_JPEG_QUALITY = 85
        private const val CHANNEL_ID = "episodive_playback_channel"

        // ServiceActions.kt (:feature:widget) 에서 하드코딩된 복제본이 동기화되어야 함.
        const val ACTION_WIDGET_PLAY_PAUSE = "io.jacob.episodive.action.WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_SEEK_FWD = "io.jacob.episodive.action.WIDGET_SEEK_FWD"
        const val ACTION_WIDGET_SEEK_BWD = "io.jacob.episodive.action.WIDGET_SEEK_BWD"
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        // 스테이션 재생 중에는 세션이 읽는 메타데이터만 프로그램 기준으로 덧씌운다. 재생 중인 항목은 건드리지 않는다.
        val player = OnAirMetadataPlayer(playerRepository.getPlayer())
        observeOnAirOverlay(player)

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
            // process 재시작 후 ExoPlayer 가 이전 세션을 이어 재생하지만 _nowPlaying 은 null 인
            // 케이스 보정: 마지막 재생 Episode 로 1회 hydrate. 같은 episode 면 내부적으로 무시됨.
            getMostRecentPlayedEpisodeUseCase()?.let { playerRepository.rehydrate(it) }
        }

        serviceScope.launch {
            getNowPlayingUseCase().collectLatest { episode ->
                nowPlaying = episode
                updateCustomLayout()
            }
        }

        serviceScope.launch {
            playerRepository.nowPlayingStation.collect { station ->
                currentStation = station
                updateCustomLayout()
            }
        }

        // 위젯 갱신은 "에피소드·스테이션 변경 또는 재생/일시정지" 시에만 트리거한다.
        // getNowPlayingUseCase 는 재생 progress 가 DB 에 저장될 때마다 재emit(초당 수십 회)되는데,
        // 그대로 notify 하면 updateAll 폭주로 provideGlance 가 완주 못 해 위젯이 옛 화면에 고정된다.
        serviceScope.launch {
            // 트리거일 뿐 판정이 아니다 — 위젯 쪽 리더가 nowPlayingStation 하나로 에피소드/스테이션을 가른다.
            // 어느 쪽이 바뀌어도 WidgetUpdater 의 debounce 가 묶어 한 번만 갱신한다.
            merge(
                getNowPlayingUseCase().map { it?.id }.distinctUntilChanged(),
                playerRepository.nowPlayingStation.map { it?.id }.distinctUntilChanged(),
                playerRepository.isPlaying.distinctUntilChanged(),
                // 위젯 부제의 프로그램 제목. Glance 세션은 곧 닫히므로 프로그램 경계에서 갱신을 건 쪽이 있어야 한다.
                // 곡은 빼고(includeSong = false) 제목만 본다 — 백그라운드 재생 내내 곡 폴링이 돌면 안 된다.
                currentProgramTitleFlow(),
            ).collectLatest { widgetUpdater.notifyNowPlayingChanged() }
        }
    }

    /**
     * 재생 중인 스테이션과 지금 방송 중인 프로그램. 일시정지 중에는 구독하지 않는다 — 멈춘 화면 때문에 편성 루프(5분 TTL
     * 재조회)를 돌릴 이유가 없고, 다시 재생하면 그때 현재 프로그램을 새로 받는다. 곡은 빼고(includeSong = false) 받는다 —
     * 백그라운드 재생 내내 곡 폴링이 돌면 안 된다.
     *
     * 위젯 갱신 트리거와 세션 메타데이터 덧씌움이 함께 쓰므로 하나로 공유한다(구독이 둘이어도 편성 루프는 하나).
     */
    private val playingProgram: Flow<Pair<Station, OnAirProgram?>> by lazy {
        combine(
            playerRepository.nowPlayingStation,
            playerRepository.isPlaying,
        ) { station, isPlaying -> station?.takeIf { isPlaying } }
            .distinctUntilChanged()
            .flatMapLatest { station ->
                if (station == null) {
                    emptyFlow()
                } else {
                    observeOnAirProgramUseCase(station.id, includeSong = false).map { station to it }
                }
            }
            .shareIn(
                scope = serviceScope,
                started = SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
                replay = 1,
            )
    }

    /** 위젯 부제가 바뀌는 신호: 프로그램 제목과 이미지. */
    private fun currentProgramTitleFlow(): Flow<Pair<String?, String?>> =
        playingProgram
            .map { (_, program) -> program?.title to program?.imageUrl }
            .distinctUntilChanged()

    /**
     * 프로그램이 바뀔 때마다 세션 메타데이터 덧씌움을 바꾼다. 프로그램을 모르면(null) 덧씌우지 않아 원래 값이 그대로다.
     *
     * - 같은 스테이션이 일시정지로 구독이 멎었다가 재개되면 마지막 덧씌움을 유지한 채 편성을 새로 평가한다 — 멈춘 알림에도
     *   프로그램이 보이는 게 맞다.
     * - 스테이션이 바뀌거나 멈추면(null) 덧씌움을 곧바로 지운다. [OnAirMetadataPlayer] 도 항목과 대조하지만, 재개했을 때 지난
     *   프로그램이 잠깐 보이지 않게 여기서 비운다.
     * - 프로그램 이미지는 먼저 텍스트만 덧씌운 뒤 받아서 바이트로 싣는다. 받기 전·실패 때는 이니셜 아트가 그대로다.
     */
    private fun observeOnAirOverlay(player: OnAirMetadataPlayer) = serviceScope.launch {
        playerRepository.nowPlayingStation
            .map { it?.id }
            .distinctUntilChanged()
            .flatMapLatest { id ->
                if (id == null) {
                    flowOf<Pair<Station, OnAirProgram?>?>(null)
                } else {
                    playingProgram
                        .filter { (station, _) -> station.id == id }
                        .map<Pair<Station, OnAirProgram?>, Pair<Station, OnAirProgram?>?> { it }
                        .onStart { emit(null) }
                }
            }
            .collectLatest { entry ->
                val (station, program) = entry ?: run { player.setOverlay(null); return@collectLatest }
                if (program == null) {
                    player.setOverlay(null)
                    return@collectLatest
                }
                val url = program.imageUrl?.takeIf { it.startsWith("https://") }
                val cached = url?.let { u -> programArtwork?.takeIf { it.url == u } }
                player.setOverlay(station.toOverlay(program, cached))
                if (url != null && cached == null) {
                    val loaded = loadProgramArtwork(url) ?: return@collectLatest
                    programArtwork = loaded
                    player.setOverlay(station.toOverlay(program, loaded))
                }
            }
    }

    // 마지막으로 받은 프로그램 이미지. 같은 프로그램이 다시 덧씌워질 때 아트가 한 번 비지 않게 한다.
    private var programArtwork: OnAirMetadataPlayer.Artwork? = null

    private fun Station.toOverlay(program: OnAirProgram, artwork: OnAirMetadataPlayer.Artwork?) =
        OnAirMetadataPlayer.Overlay(
            station = this,
            artist = program.host?.takeIf { it.isNotBlank() }?.let { "${program.title} · $it" } ?: program.title,
            artwork = artwork,
        )

    /** Coil 로 받아 알림 큰 아이콘에 알맞은 크기로 줄여 JPEG 바이트로 만든다. 실패하면 null. */
    private suspend fun loadProgramArtwork(url: String): OnAirMetadataPlayer.Artwork? {
        return try {
            val request = ImageRequest.Builder(applicationContext)
                .data(url)
                .size(ARTWORK_SIZE_PX)
                .allowHardware(false)
                .build()
            val result = Coil.imageLoader(applicationContext).execute(request)
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return null
            val bytes = withContext(Dispatchers.Default) {
                ByteArrayOutputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, ARTWORK_JPEG_QUALITY, out)
                    out.toByteArray()
                }
            }
            OnAirMetadataPlayer.Artwork(url, bytes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "program artwork load failed: $url")
            null
        }
    }

    @UnstableApi
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            // 플레이어가 비어 있으면(스테이션 정지 직후 등) 마지막 재생 상태를 올린 뒤 튼다.
            ACTION_WIDGET_PLAY_PAUSE -> serviceScope.launch {
                if (!widgetPlayPauseUseCase()) Timber.d("Widget play ignored: nothing to play or restore")
            }
            ACTION_WIDGET_SEEK_FWD -> if (currentControls().showCustomButtons) {
                serviceScope.launch { playerRepository.seekForward() }
            }

            ACTION_WIDGET_SEEK_BWD -> if (currentControls().showCustomButtons) {
                serviceScope.launch { playerRepository.seekBackward() }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun currentControls(): NotificationControls =
        notificationControls(currentStation, nowPlaying?.isLiked ?: false)

    private fun updateCustomLayout() {
        mediaSession?.setCustomLayout(getCustomLayout())
    }

    private fun getCustomLayout(): ImmutableList<CommandButton> {
        val controls = currentControls()
        // 라이브에는 15초/30초 건너뛰기도 좋아요 대상도 없다.
        if (!controls.showCustomButtons) return ImmutableList.of()
        return getCustomLayout(controls.isLiked)
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
            // 레이아웃을 비워 두어도 이미 그려진 알림이나 외부 컨트롤러가 명령을 보낼 수 있다.
            // 무시한 것을 성공으로 답하면 컨트롤러는 처리된 줄 안다 — 지원하지 않는다고 답한다.
            // (홈 화면 위젯은 스테이션이면 ±탐색 버튼을 그리지 않는다.)
            if (!currentControls().showCustomButtons) {
                return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
            }
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
        nowPlaying?.let { toggleLikedEpisodeUseCase(it) }
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