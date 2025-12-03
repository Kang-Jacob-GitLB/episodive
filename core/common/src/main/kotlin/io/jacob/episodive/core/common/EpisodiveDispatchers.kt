package io.jacob.episodive.core.common

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Dispatcher(val episodiveDispatcher: EpisodiveDispatchers)

enum class EpisodiveDispatchers {
    Default,
    IO,
}