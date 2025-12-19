package io.jacob.episodive.core.testing.util

import androidx.paging.PagingSource
import org.junit.Assert.assertTrue

/**
 * Loads data from PagingSource and returns the page data for testing.
 * Similar to Flow.test{} and PagingData.asSnapshot{}.
 *
 * @param key The key for the page to load (null for initial load)
 * @param loadSize The number of items to load
 * @param placeholdersEnabled Whether placeholders are enabled
 * @return List of loaded items
 * @throws AssertionError if the load result is not a Page
 *
 * Example:
 * ```
 * val data = pagingSource.loadAsSnapshot(loadSize = 10)
 * assertEquals(10, data.size)
 * ```
 */
suspend fun <Key : Any, Value : Any> PagingSource<Key, Value>.loadAsSnapshot(
    key: Key? = null,
    loadSize: Int = 10,
    placeholdersEnabled: Boolean = false,
): List<Value> {
    val loadResult = load(
        PagingSource.LoadParams.Refresh(
            key = key,
            loadSize = loadSize,
            placeholdersEnabled = placeholdersEnabled
        )
    )

    assertTrue(
        "Expected LoadResult.Page but got ${loadResult::class.simpleName}",
        loadResult is PagingSource.LoadResult.Page
    )

    return (loadResult as PagingSource.LoadResult.Page).data
}

/**
 * Loads data from PagingSource and provides the full LoadResult.Page for detailed testing.
 * Use this when you need to test prevKey, nextKey, or other page properties.
 *
 * @param key The key for the page to load (null for initial load)
 * @param loadSize The number of items to load
 * @param placeholdersEnabled Whether placeholders are enabled
 * @return LoadResult.Page with data and keys
 * @throws AssertionError if the load result is not a Page
 *
 * Example:
 * ```
 * val page = pagingSource.loadPage(loadSize = 10)
 * assertEquals(10, page.data.size)
 * assertNull(page.prevKey)
 * assertNotNull(page.nextKey)
 * ```
 */
suspend fun <Key : Any, Value : Any> PagingSource<Key, Value>.loadPage(
    key: Key? = null,
    loadSize: Int = 10,
    placeholdersEnabled: Boolean = false,
): PagingSource.LoadResult.Page<Key, Value> {
    val loadResult = load(
        PagingSource.LoadParams.Refresh(
            key = key,
            loadSize = loadSize,
            placeholdersEnabled = placeholdersEnabled
        )
    )

    assertTrue(
        "Expected LoadResult.Page but got ${loadResult::class.simpleName}",
        loadResult is PagingSource.LoadResult.Page
    )

    return loadResult as PagingSource.LoadResult.Page<Key, Value>
}

/**
 * Loads the next page (append) from PagingSource.
 *
 * @param key The key for the next page
 * @param loadSize The number of items to load
 * @param placeholdersEnabled Whether placeholders are enabled
 * @return List of loaded items
 * @throws AssertionError if the load result is not a Page
 *
 * Example:
 * ```
 * val firstPage = pagingSource.loadAsSnapshot()
 * val nextKey = pagingSource.loadPage().nextKey
 * val secondPage = pagingSource.loadNextPage(key = nextKey)
 * ```
 */
suspend fun <Key : Any, Value : Any> PagingSource<Key, Value>.loadNextPage(
    key: Key,
    loadSize: Int = 10,
    placeholdersEnabled: Boolean = false,
): List<Value> {
    val loadResult = load(
        PagingSource.LoadParams.Append(
            key = key,
            loadSize = loadSize,
            placeholdersEnabled = placeholdersEnabled
        )
    )

    assertTrue(
        "Expected LoadResult.Page but got ${loadResult::class.simpleName}",
        loadResult is PagingSource.LoadResult.Page
    )

    return (loadResult as PagingSource.LoadResult.Page).data
}

/**
 * Loads the previous page (prepend) from PagingSource.
 *
 * @param key The key for the previous page
 * @param loadSize The number of items to load
 * @param placeholdersEnabled Whether placeholders are enabled
 * @return List of loaded items
 * @throws AssertionError if the load result is not a Page
 */
suspend fun <Key : Any, Value : Any> PagingSource<Key, Value>.loadPrevPage(
    key: Key,
    loadSize: Int = 10,
    placeholdersEnabled: Boolean = false,
): List<Value> {
    val loadResult = load(
        PagingSource.LoadParams.Prepend(
            key = key,
            loadSize = loadSize,
            placeholdersEnabled = placeholdersEnabled
        )
    )

    assertTrue(
        "Expected LoadResult.Page but got ${loadResult::class.simpleName}",
        loadResult is PagingSource.LoadResult.Page
    )

    return (loadResult as PagingSource.LoadResult.Page).data
}