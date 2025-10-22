package io.jacob.episodive.core.domain.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainPlayerRepository

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ClipPlayerRepository
