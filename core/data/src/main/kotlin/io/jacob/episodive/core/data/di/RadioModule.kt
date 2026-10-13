package io.jacob.episodive.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.jacob.episodive.core.common.ApplicationScope
import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.common.TimeProvider
import io.jacob.episodive.core.data.player.SleepTimerImpl
import io.jacob.episodive.core.data.radio.LiveStreamSupervisorImpl
import io.jacob.episodive.core.data.radio.StationPlaybackControllerImpl
import io.jacob.episodive.core.data.util.NetworkMonitor
import io.jacob.episodive.core.domain.player.SleepTimer
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.domain.station.LiveStreamSupervisor
import io.jacob.episodive.core.domain.station.StationPlaybackController
import io.jacob.episodive.core.domain.usecase.station.PlayStationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RadioModule {
    /**
     * ApplicationScope 는 Default 에서 돈다. 플레이어는 메인에서만 만질 수 있으므로 감독자는
     * 메인 디스패처로 갈아타서 일한다.
     */
    @Provides
    @Singleton
    fun provideLiveStreamSupervisor(
        @Player(EpisodivePlayers.Main) playerRepository: PlayerRepository,
        stationRepository: StationRepository,
        networkMonitor: NetworkMonitor,
        @ApplicationScope scope: CoroutineScope,
    ): LiveStreamSupervisor {
        return LiveStreamSupervisorImpl(
            playerRepository = playerRepository,
            stationRepository = stationRepository,
            networkMonitor = networkMonitor,
            scope = scope,
            mainDispatcher = Dispatchers.Main.immediate,
        )
    }

    @Provides
    @Singleton
    fun provideStationPlaybackController(
        playStationUseCase: PlayStationUseCase,
        @Player(EpisodivePlayers.Main) playerRepository: PlayerRepository,
        stationRepository: StationRepository,
        @ApplicationScope scope: CoroutineScope,
    ): StationPlaybackController {
        return StationPlaybackControllerImpl(
            playStationUseCase = playStationUseCase,
            playerRepository = playerRepository,
            stationRepository = stationRepository,
            scope = scope,
            mainDispatcher = Dispatchers.Main.immediate,
        )
    }

    /** 타이머는 화면이 아니라 앱 수명으로 산다 — 시트가 닫히거나 에피소드/스테이션이 바뀌어도 이어진다. */
    @Provides
    @Singleton
    fun provideSleepTimer(
        @Player(EpisodivePlayers.Main) playerRepository: PlayerRepository,
        timeProvider: TimeProvider,
        @ApplicationScope scope: CoroutineScope,
    ): SleepTimer {
        return SleepTimerImpl(
            playerRepository = playerRepository,
            timeProvider = timeProvider,
            scope = scope,
            mainDispatcher = Dispatchers.Main.immediate,
        )
    }
}
