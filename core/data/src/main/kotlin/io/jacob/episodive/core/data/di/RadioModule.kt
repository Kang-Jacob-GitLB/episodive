package io.jacob.episodive.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.jacob.episodive.core.common.ApplicationScope
import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.data.radio.LiveStreamSupervisorImpl
import io.jacob.episodive.core.data.radio.StationPlaybackControllerImpl
import io.jacob.episodive.core.data.util.NetworkMonitor
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
}
