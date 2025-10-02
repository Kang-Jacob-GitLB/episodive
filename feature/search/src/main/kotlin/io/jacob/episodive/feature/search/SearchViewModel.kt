package io.jacob.episodive.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.domain.usecase.SearchUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetRecentEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.feed.GetTrendingFeedsUseCase
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.SearchResult
import io.jacob.episodive.core.model.TrendingFeed
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchUseCase: SearchUseCase,
    private val getRecentEpisodesUseCase: GetRecentEpisodesUseCase,
    private val getTrendingFeedsUseCase: GetTrendingFeedsUseCase,
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    private val _searchResult: Flow<SearchResult> = _searchQuery
        .debounce(500L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isNotEmpty()) {
                searchUseCase(query)
            } else {
                flowOf(SearchResult())
            }
        }

    val state: StateFlow<SearchState> = combine(
        _searchQuery,
        _searchResult,
        getRecentEpisodesUseCase(),
        getTrendingFeedsUseCase(),
    ) { query, result, recentEpisodes, trendingFeeds ->
        SearchState.Success(
            searchQuery = query,
            searchHistory = emptyList(), // Implement search history if needed
            searchResult = result,
            categories = Category.entries.toList(),
            recentEpisodes = recentEpisodes,
            trendingFeeds = trendingFeeds,
        ) as SearchState
    }.catch { e ->
        emit(SearchState.Error(e.message ?: "An unknown error occurred"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchState.Loading
    )

    private val _action = MutableSharedFlow<SearchAction>(extraBufferCapacity = 1)

    private val _effect = MutableSharedFlow<SearchEffect>(extraBufferCapacity = 1)
    val effect = _effect.asSharedFlow()

    init {
        handleActions()
    }

    private fun handleActions() = viewModelScope.launch {
        _action.collect { action ->
            when (action) {
                is SearchAction.QueryChanged -> {
                    _searchQuery.value = action.query
                }

                is SearchAction.ClickSearch -> {
                    _searchQuery.value = action.query
                }

                is SearchAction.ClearQuery -> {
                    _searchQuery.value = ""
                }

                is SearchAction.ClickRecentSearch -> {
                    _searchQuery.value = action.query
                }

                is SearchAction.RemoveRecentSearch -> {
                    // Implement removal from search history if needed
                }

                is SearchAction.ClearRecentSearches -> {
                    // Implement clearing of search history if needed
                }

                is SearchAction.ClickCategory -> {
                    _effect.emit(SearchEffect.NavigateToCategory(action.category))
                }

                is SearchAction.ClickPodcast -> {
                    _effect.emit(SearchEffect.NavigateToPodcast(action.podcast))
                }

                is SearchAction.ClickEpisode -> {
                    _effect.emit(SearchEffect.NavigateToEpisode(action.episode))
                }
            }
        }
    }

    fun sendAction(action: SearchAction) = viewModelScope.launch {
        _action.emit(action)
    }
}

sealed interface SearchState {
    data object Loading : SearchState
    data class Success(
        val searchQuery: String,
        val searchHistory: List<String>,
        val searchResult: SearchResult,
        val categories: List<Category>,
        val recentEpisodes: List<Episode>,
        val trendingFeeds: List<TrendingFeed>,
    ) : SearchState

    data class Error(val message: String) : SearchState
}

sealed interface SearchAction {
    data class QueryChanged(val query: String) : SearchAction
    data class ClickSearch(val query: String) : SearchAction
    data object ClearQuery : SearchAction
    data class ClickRecentSearch(val query: String) : SearchAction
    data class RemoveRecentSearch(val query: String) : SearchAction
    data object ClearRecentSearches : SearchAction
    data class ClickCategory(val category: Category) : SearchAction
    data class ClickPodcast(val podcast: Podcast) : SearchAction
    data class ClickEpisode(val episode: Episode) : SearchAction
}

sealed interface SearchEffect {
    data class NavigateToCategory(val category: Category) : SearchEffect
    data class NavigateToPodcast(val podcast: Podcast) : SearchEffect
    data class NavigateToEpisode(val episode: Episode) : SearchEffect
}