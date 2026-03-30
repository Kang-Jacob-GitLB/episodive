package io.jacob.episodive.core.data.util.paging

import androidx.paging.PagingConfig

object PagingDefaults {
    val DEFAULT_CONFIG = PagingConfig(
        pageSize = 20,
        prefetchDistance = 5,
        enablePlaceholders = false,
    )
}
