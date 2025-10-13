package io.jacob.episodive.feature.search

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.jacob.episodive.core.designsystem.component.EpisodeItem
import io.jacob.episodive.core.designsystem.component.EpisodesSection
import io.jacob.episodive.core.designsystem.component.EpisodiveSearchBar
import io.jacob.episodive.core.designsystem.component.FeedsSection
import io.jacob.episodive.core.designsystem.component.PodcastsSection
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Feed
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.SearchResult
import io.jacob.episodive.core.model.mapper.toFeedsFromTrending
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.testing.model.trendingFeedTestDataList

@Composable
fun SearchRoute(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
    onPodcastClick: (Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SearchEffect.NavigateToCategory -> {}
                is SearchEffect.NavigateToPodcast -> {
                    onPodcastClick(effect.podcast.id)
                }

                is SearchEffect.NavigateToEpisode -> {}
            }
        }
    }

    when (val s = state) {
        is SearchState.Loading -> {}
        is SearchState.Success -> {
            SearchScreen(
                modifier = modifier,
                query = s.searchQuery,
                onQueryChange = { viewModel.sendAction(SearchAction.QueryChanged(it)) },
                onSearch = { viewModel.sendAction(SearchAction.ClickSearch(it)) },
                searchResult = s.searchResult,
                episodes = s.recentEpisodes,
                feeds = s.trendingFeeds.toFeedsFromTrending(),
                onPodcastClick = { viewModel.sendAction(SearchAction.ClickPodcast(it)) },
                onEpisodeClick = { viewModel.sendAction(SearchAction.ClickEpisode(it)) },
            )
        }

        is SearchState.Error -> {}
    }
}

@Composable
private fun SearchScreen(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    searchResult: SearchResult,
    feeds: List<Feed>,
    episodes: List<Episode>,
    onPodcastClick: (Podcast) -> Unit = {},
    onEpisodeClick: (Episode) -> Unit = {},
    onFeedClick: (Feed) -> Unit = {},
) {
    EpisodiveSearchBar(
        modifier = modifier,
        query = query,
        onQueryChange = onQueryChange,
        onSearch = onSearch,
//        searchResult = searchResult,
//        onPodcastClick = onPodcastClick,
//        onEpisodeClick = onEpisodeClick,
        contentOnCollapse = {
            SearchContentsOnCollapse(
                modifier = Modifier,
                episodes = episodes,
                feeds = feeds,
                onEpisodeClick = onEpisodeClick,
                onFeedClick = onFeedClick,
            )
        },
        contentOnExpand = { scrollState ->
            SearchResultsOnExpand(
                scrollState = scrollState,
                searchResult = searchResult,
                onPodcastClick = onPodcastClick,
                onEpisodeClick = onEpisodeClick,
            )
        }
    )
}

@Composable
private fun SearchContentsOnCollapse(
    modifier: Modifier = Modifier,
    feeds: List<Feed>,
    episodes: List<Episode>,
    onEpisodeClick: (Episode) -> Unit = {},
    onFeedClick: (Feed) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        if (feeds.isNotEmpty()) {
            item {
                FeedsSection(
                    modifier = Modifier
                        .fillMaxWidth(),
                    title = "Feeds",
                    feeds = feeds,
                    onFeedClick = onFeedClick
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
                    title = "Episodes",
                    episodes = episodes,
                    onEpisodeClick = onEpisodeClick
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
    searchResult: SearchResult,
    onPodcastClick: (Podcast) -> Unit = {},
    onEpisodeClick: (Episode) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth(),
        state = scrollState
    ) {
        if (searchResult.podcasts.isNotEmpty()) {
            item {
                PodcastsSection(
                    title = "Podcasts",
                    podcasts = searchResult.podcasts,
                    onMore = {},
                    onPodcastClick = onPodcastClick,
                )
            }
        } else {
            item {
                Text(
                    text = "No podcasts found",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
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
                    title = "Episodes",
                    episodes = emptyList(),
                    onEpisodeClick = {}
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
                    onClick = { onEpisodeClick(episode) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(LocalDimensionTheme.current.playerBarHeight))
        }
    }
}

@DevicePreviews
@Composable
private fun SearchScreenPreview() {
    EpisodiveTheme {
        SearchScreen(
            query = "test",
            onQueryChange = {},
            onSearch = {},
            searchResult = SearchResult(
                podcasts = podcastTestDataList.take(3),
                episodes = episodeTestDataList,
            ),
            feeds = trendingFeedTestDataList.toFeedsFromTrending(),
            episodes = episodeTestDataList,
        )
    }
}