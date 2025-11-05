package io.jacob.episodive.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.jacob.episodive.core.designsystem.component.EpisodeItem
import io.jacob.episodive.core.designsystem.component.EpisodesSection
import io.jacob.episodive.core.designsystem.component.EpisodiveScaffold
import io.jacob.episodive.core.designsystem.component.EpisodiveSearchBar
import io.jacob.episodive.core.designsystem.component.PodcastsSection
import io.jacob.episodive.core.designsystem.component.PodcastsWithAuthorSection
import io.jacob.episodive.core.designsystem.component.SectionHeader
import io.jacob.episodive.core.designsystem.component.scrollbar.DraggableScrollbar
import io.jacob.episodive.core.designsystem.component.scrollbar.scrollbarState
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.screen.ErrorScreen
import io.jacob.episodive.core.designsystem.screen.LoadingScreen
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.SearchResult
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestDataList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun SearchRoute(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
    onPodcastClick: (Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is SearchEffect.NavigateToCategory -> {}
                is SearchEffect.NavigateToPodcast -> onPodcastClick(effect.podcastId)
                is SearchEffect.NavigateToEpisode -> {}
            }
        }
    }

    when (val s = state) {
        is SearchState.Loading -> LoadingScreen()
        is SearchState.Success -> {
            SearchScreen(
                modifier = modifier,
                query = s.searchQuery,
                onQueryChange = { viewModel.sendAction(SearchAction.QueryChanged(it)) },
                onSearch = { viewModel.sendAction(SearchAction.ClickSearch(it)) },
                recentSearches = s.recentSearches,
                searchResult = s.searchResult,
                episodes = s.recentEpisodes,
                podcasts = s.trendingPodcasts,
                onPodcastClick = { viewModel.sendAction(SearchAction.ClickPodcast(it)) },
                onEpisodeClick = { viewModel.sendAction(SearchAction.ClickEpisode(it)) },
                onToggleEpisodeLiked = { viewModel.sendAction(SearchAction.ToggleEpisodeLiked(it)) },
                onRecentSearchClick = { viewModel.sendAction(SearchAction.ClickRecentSearch(it)) },
                onRemoveRecentSearch = { viewModel.sendAction(SearchAction.RemoveRecentSearch(it)) },
                onClearRecentSearches = { viewModel.sendAction(SearchAction.ClearRecentSearches) },
            )
        }

        is SearchState.Error -> ErrorScreen(message = s.message)
    }
}

@Composable
private fun SearchScreen(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    recentSearches: List<String>,
    searchResult: SearchResult,
    podcasts: List<Podcast>,
    episodes: List<Episode>,
    onPodcastClick: (Podcast) -> Unit = {},
    onEpisodeClick: (Episode) -> Unit = {},
    onToggleEpisodeLiked: (Episode) -> Unit = {},
    onRecentSearchClick: (String) -> Unit = {},
    onRemoveRecentSearch: (String) -> Unit = {},
    onClearRecentSearches: () -> Unit = {},
    isExpanded: Boolean = false,
) {
    EpisodiveScaffold(
        modifier = modifier,
        title = stringResource(R.string.feature_search_title),
    ) { paddingValues, nestedScrollConnection ->
        EpisodiveSearchBar(
            modifier = modifier
                .padding(paddingValues),
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            isExpanded = isExpanded,
            placeholder = {
                Text(stringResource(R.string.feature_search_placeholder))
            },
            contentOnCollapse = {
                SearchContentsOnCollapse(
                    modifier = Modifier,
                    episodes = episodes,
                    podcasts = podcasts,
                    onEpisodeClick = onEpisodeClick,
                    onToggleEpisodeLiked = onToggleEpisodeLiked,
                    onPodcastClick = onPodcastClick,
                )
            },
            contentOnExpand = { scrollState ->
                SearchResultsOnExpand(
                    scrollState = scrollState,
                    recentSearches = recentSearches,
                    searchResult = searchResult,
                    onPodcastClick = onPodcastClick,
                    onEpisodeClick = onEpisodeClick,
                    onToggleEpisodeLiked = onToggleEpisodeLiked,
                    onRecentSearchClick = onRecentSearchClick,
                    onRemoveRecentSearch = onRemoveRecentSearch,
                    onClearRecentSearches = onClearRecentSearches
                )
            }
        )
    }
}

@Composable
private fun SearchContentsOnCollapse(
    modifier: Modifier = Modifier,
    podcasts: List<Podcast>,
    episodes: List<Episode>,
    onEpisodeClick: (Episode) -> Unit = {},
    onToggleEpisodeLiked: (Episode) -> Unit = {},
    onPodcastClick: (Podcast) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        if (podcasts.isNotEmpty()) {
            item {
                PodcastsWithAuthorSection(
                    modifier = Modifier
                        .fillMaxWidth(),
                    title = stringResource(R.string.feature_search_section_global_trending_feeds),
                    podcasts = podcasts,
                    onPodcastClick = onPodcastClick
                )
            }
        }

        if (episodes.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(12.dp))
            }

            item {
                EpisodesSection(
                    modifier = Modifier
                        .fillMaxWidth(),
                    title = stringResource(R.string.feature_search_section_global_recent_episodes),
                    episodes = episodes,
                    onEpisodeClick = onEpisodeClick,
                    onToggleEpisodeLiked = onToggleEpisodeLiked,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(LocalDimensionTheme.current.playerBarHeight))
        }
    }
}

@Composable
private fun SearchResultsOnExpand(
    modifier: Modifier = Modifier,
    scrollState: LazyListState,
    recentSearches: List<String>,
    searchResult: SearchResult,
    onPodcastClick: (Podcast) -> Unit = {},
    onEpisodeClick: (Episode) -> Unit = {},
    onToggleEpisodeLiked: (Episode) -> Unit = {},
    onRecentSearchClick: (String) -> Unit = {},
    onRemoveRecentSearch: (String) -> Unit = {},
    onClearRecentSearches: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = modifier
                .fillMaxWidth(),
            state = scrollState
        ) {
            if (searchResult.podcasts.isNotEmpty()) {
                item {
                    PodcastsSection(
                        title = stringResource(R.string.feature_search_section_podcasts),
                        podcasts = searchResult.podcasts,
                        onMore = {},
                        onPodcastClick = onPodcastClick,
                    )
                }
            } else {
                item {
                    RecentSearchesSection(
                        title = stringResource(R.string.feature_search_section_recent_searches),
                        recentSearches = recentSearches,
                        onRecentSearchClicked = onRecentSearchClick,
                        onRemoveRecentSearch = onRemoveRecentSearch,
                        onClearRecentSearches = onClearRecentSearches
                    )
                }
            }

            if (searchResult.episodes.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(12.dp))
                }

                item {
                    EpisodesSection(
                        modifier = Modifier
                            .fillMaxWidth(),
                        title = stringResource(R.string.feature_search_section_episodes),
                        episodes = emptyList(),
                        onEpisodeClick = onEpisodeClick,
                        onToggleEpisodeLiked = onToggleEpisodeLiked,
                    )
                }

                items(
                    count = searchResult.episodes.size,
                    key = { searchResult.episodes[it].id },
                ) { index ->
                    val episode = searchResult.episodes[index]

                    EpisodeItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        episode = episode,
                        isLoading = false,
                        onClick = { onEpisodeClick(episode) },
                        onToggleLiked = { onToggleEpisodeLiked(episode) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(LocalDimensionTheme.current.playerBarHeight))
            }
        }

        scrollState.DraggableScrollbar(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 12.dp)
                .align(Alignment.TopEnd),
            state = scrollState.scrollbarState(itemsAvailable = searchResult.episodes.size),
            orientation = Orientation.Vertical,
            onThumbMoved = { thumbPosition ->
                scope.launch {
                    val itemIndex = (thumbPosition * searchResult.episodes.size).toInt()
                        .coerceIn(0, searchResult.episodes.size - 1)
                    scrollState.scrollToItem(itemIndex)
                }
            }
        )
    }
}

@Composable
private fun RecentSearchesSection(
    modifier: Modifier = Modifier,
    title: String,
    recentSearches: List<String>,
    onRecentSearchClicked: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit
) {
    SectionHeader(
        modifier = modifier,
        title = title,
        actionIcon = EpisodiveIcons.Close,
        actionIconContentDescription = "Clear recent searches",
        onActionClick = onClearRecentSearches,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            recentSearches.forEach { recentSearch ->
                RecentSearchItem(
                    recentSearch = recentSearch,
                    onClick = { onRecentSearchClicked(recentSearch) },
                    onRemove = { onRemoveRecentSearch(recentSearch) }
                )
            }
        }
    }
}

@Composable
private fun RecentSearchItem(
    modifier: Modifier = Modifier,
    recentSearch: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = EpisodiveIcons.History,
            contentDescription = "Recent Search Icon",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = recentSearch,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = { onRemove() }) {
            Icon(
                imageVector = EpisodiveIcons.Close,
                contentDescription = "Remove Recent Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@DevicePreviews
@Composable
private fun SearchScreenOnCollapsePreview() {
    EpisodiveTheme {
        SearchScreen(
            query = "test",
            onQueryChange = {},
            onSearch = {},
            recentSearches = listOf("test1", "test2", "test3", "test4", "test5"),
            searchResult = SearchResult(
                podcasts = podcastTestDataList.take(3),
                episodes = episodeTestDataList,
            ),
            podcasts = podcastTestDataList,
            episodes = episodeTestDataList,
        )
    }
}

@DevicePreviews
@Composable
private fun SearchScreenOnExpandPreview() {
    EpisodiveTheme {
        SearchScreen(
            query = "test",
            onQueryChange = {},
            onSearch = {},
            recentSearches = listOf("test1", "test2", "test3", "test4", "test5"),
            searchResult = SearchResult(
                podcasts = podcastTestDataList.take(3),
                episodes = episodeTestDataList,
            ),
            podcasts = podcastTestDataList,
            episodes = episodeTestDataList,
            isExpanded = true,
        )
    }
}

@DevicePreviews
@Composable
private fun SearchScreenOnExpandRecentSearchPreview() {
    EpisodiveTheme {
        SearchScreen(
            query = "test",
            onQueryChange = {},
            onSearch = {},
            recentSearches = listOf("test1", "test2", "test3", "test4", "test5"),
            searchResult = SearchResult(
                podcasts = emptyList(),
                episodes = emptyList(),
            ),
            podcasts = podcastTestDataList,
            episodes = episodeTestDataList,
            isExpanded = true,
        )
    }
}