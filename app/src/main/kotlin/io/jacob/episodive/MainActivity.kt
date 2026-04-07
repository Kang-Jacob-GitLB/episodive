package io.jacob.episodive

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
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
import io.jacob.episodive.core.data.util.NetworkMonitor
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.ui.EpisodiveApp
import io.jacob.episodive.ui.rememberEpisodiveAppState
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var networkMonitor: NetworkMonitor

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
    }

    override fun onDestroy() {
        super.onDestroy()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}