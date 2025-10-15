package io.jacob.episodive.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.domain.usecase.episode.GetRecentEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.feed.GetTrendingFeedsUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.search.ClearRecentSearchesUseCase
import io.jacob.episodive.core.domain.usecase.search.DeleteRecentSearchUseCase
import io.jacob.episodive.core.domain.usecase.search.GetRecentSearchesUseCase
import io.jacob.episodive.core.domain.usecase.search.SearchUseCase
import io.jacob.episodive.core.domain.usecase.search.UpsertRecentSearchUseCase
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Feed
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
import kotlinx.coroutines.flow.collectLatest
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
    getRecentEpisodesUseCase: GetRecentEpisodesUseCase,
    getTrendingFeedsUseCase: GetTrendingFeedsUseCase,
    private val playEpisodeUseCase: PlayEpisodeUseCase,
    getRecentSearchesUseCase: GetRecentSearchesUseCase,
    private val upsertRecentSearchUseCase: UpsertRecentSearchUseCase,
    private val deleteRecentSearchUseCase: DeleteRecentSearchUseCase,
    private val clearRecentSearchesUseCase: ClearRecentSearchesUseCase,
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
        getRecentSearchesUseCase(),
        getRecentEpisodesUseCase(),
        getTrendingFeedsUseCase(),
    ) { query, result, recentSearches, recentEpisodes, trendingFeeds ->
        SearchState.Success(
            searchQuery = query,
            searchHistory = emptyList(), // Implement search history if needed
            searchResult = result,
            recentSearches = recentSearches,
            categories = Category.entries.toList(),
            recentEpisodes = recentEpisodes.take(6),
            trendingFeeds = trendingFeeds.take(10),
        ) as SearchState
    }.catch { e ->
        emit(SearchState.Error(e.message ?: "An unknown error occurred"))
        e.printStackTrace()
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
        _action.collectLatest { action ->
            when (action) {
                is SearchAction.QueryChanged -> changeQuery(action.query)
                is SearchAction.ClickSearch -> search(action.query)
                is SearchAction.ClearQuery -> clearQuery()
                is SearchAction.ClickRecentSearch -> search(action.query)
                is SearchAction.RemoveRecentSearch -> removeRecentSearch(action.query)
                is SearchAction.ClearRecentSearches -> clearRecentSearches()
                is SearchAction.ClickCategory -> clickCategory(action.category)
                is SearchAction.ClickPodcast -> clickPodcast(action.podcast)
                is SearchAction.ClickEpisode -> clickEpisode(action.episode)
                is SearchAction.ClickFeed -> clickFeed(action.feed)
            }
        }
    }

    fun sendAction(action: SearchAction) = viewModelScope.launch {
        _action.emit(action)
    }

    private fun changeQuery(query: String) = viewModelScope.launch {
        _searchQuery.emit(query)
    }

    private fun search(query: String) = viewModelScope.launch {
        _searchQuery.emit(query)
        upsertRecentSearchUseCase(query)
    }

    private fun clearQuery() = viewModelScope.launch {
        _searchQuery.emit("")
    }

    private fun removeRecentSearch(query: String) = viewModelScope.launch {
        deleteRecentSearchUseCase(query)
    }

    private fun clearRecentSearches() = viewModelScope.launch {
        clearRecentSearchesUseCase()
    }

    private fun clickCategory(category: Category) = viewModelScope.launch {
        _effect.emit(SearchEffect.NavigateToCategory(category))
    }

    private fun clickPodcast(podcast: Podcast) = viewModelScope.launch {
        _effect.emit(SearchEffect.NavigateToPodcast(podcast.id))
    }

    private fun clickEpisode(episode: Episode) = viewModelScope.launch {
        playEpisodeUseCase(episode)
//        _effect.emit(SearchEffect.NavigateToEpisode(episode))
    }

    private fun clickFeed(feed: Feed) = viewModelScope.launch {
        _effect.emit(SearchEffect.NavigateToPodcast(feed.id))
    }
}

sealed interface SearchState {
    data object Loading : SearchState
    data class Success(
        val searchQuery: String,
        val searchHistory: List<String>,
        val searchResult: SearchResult,
        val recentSearches: List<String>,
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
    data class ClickFeed(val feed: Feed) : SearchAction
}

sealed interface SearchEffect {
    data class NavigateToCategory(val category: Category) : SearchEffect
    data class NavigateToPodcast(val podcastId: Long) : SearchEffect
    data class NavigateToEpisode(val episode: Episode) : SearchEffect
}