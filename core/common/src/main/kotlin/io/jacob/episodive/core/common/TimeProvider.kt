package io.jacob.episodive.core.common

import javax.inject.Inject

interface TimeProvider {
    fun currentTimeMillis(): Long
}

class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
