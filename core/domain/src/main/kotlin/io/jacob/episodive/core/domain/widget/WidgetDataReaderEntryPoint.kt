package io.jacob.episodive.core.domain.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetDataReaderEntryPoint {
    fun widgetDataReader(): WidgetDataReader
}
