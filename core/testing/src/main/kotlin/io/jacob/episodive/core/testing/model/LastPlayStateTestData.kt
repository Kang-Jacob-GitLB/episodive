package io.jacob.episodive.core.testing.model

import io.jacob.episodive.core.model.LastPlayState
import io.jacob.episodive.core.model.Repeat

val lastPlayStateTestData = LastPlayState(
    episodeId = 1L,
    index = 0,
    positionMs = 0L,
    shuffle = false,
    repeat = Repeat.OFF,
)
