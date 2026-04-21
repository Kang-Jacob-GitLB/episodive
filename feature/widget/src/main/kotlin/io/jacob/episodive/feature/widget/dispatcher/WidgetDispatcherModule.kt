package io.jacob.episodive.feature.widget.dispatcher

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.jacob.episodive.core.data.widget.WidgetDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetDispatcherModule {
    @Binds
    @Singleton
    abstract fun bindsWidgetDispatcher(impl: GlanceWidgetDispatcher): WidgetDispatcher
}
