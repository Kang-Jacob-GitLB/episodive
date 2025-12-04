package io.jacob.episodive.core.common

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Player(val episodivePlayer: EpisodivePlayers)

enum class EpisodivePlayers {
    Main,
    Clip,
}