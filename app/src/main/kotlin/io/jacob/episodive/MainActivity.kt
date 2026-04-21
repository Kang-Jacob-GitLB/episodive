package io.jacob.episodive

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.tracing.trace
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.data.util.NetworkMonitor
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.ui.EpisodiveApp
import io.jacob.episodive.ui.rememberEpisodiveAppState
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    @Player(EpisodivePlayers.Main)
    lateinit var playerRepository: PlayerRepository

    private val viewModel: MainActivityViewModel by viewModels()

    private var controllerFuture: ListenableFuture<MediaController>? = null

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition {
            viewModel.state.value.shouldKeepSplashScreen()
        }

        viewModel.handleDeepLink(intent)

        // Start and bind MediaSessionService
        val intent = Intent(this, MediaNotificationService::class.java)
        startService(intent)

        val sessionToken =
            SessionToken(this, ComponentName(this, MediaNotificationService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            // MediaController connected
        }, MoreExecutors.directExecutor())

        handleWidgetAutoplay(getIntent())

        setContent {
            val appState = rememberEpisodiveAppState(
                networkMonitor = networkMonitor,
                viewModel = viewModel,
            )
            EpisodiveTheme {
                EpisodiveApp(appState)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.handleDeepLink(intent)
        handleWidgetAutoplay(intent)
    }

    private fun handleWidgetAutoplay(intent: Intent) {
        if (!intent.getBooleanExtra(EXTRA_WIDGET_AUTOPLAY, false)) return

        trace("widget.autoplay") {
            val startMs = SystemClock.uptimeMillis()
            val svc = Intent(this, MediaNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svc)
            } else {
                startService(svc)
            }
            playerRepository.playOrPause()
            intent.removeExtra(EXTRA_WIDGET_AUTOPLAY)
            val deltaMs = SystemClock.uptimeMillis() - startMs
            Log.d("WidgetPerf", "autoplay deltaMs=$deltaMs")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }

    companion object {
        // ServiceActions.kt (:feature:widget) 에서 하드코딩된 복제본이 동기화되어야 함.
        const val EXTRA_WIDGET_AUTOPLAY = "widget_autoplay"
    }
}