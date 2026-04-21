package io.jacob.episodive.core.data.widget

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.jacob.episodive.core.domain.widget.WidgetDataReader
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetDataModule {
    @Binds
    @Singleton
    abstract fun bindsWidgetDataReader(impl: WidgetDataReaderImpl): WidgetDataReader
}
