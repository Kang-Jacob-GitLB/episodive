package io.jacob.episodive.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.jacob.episodive.core.designsystem.component.EpisodeItem
import io.jacob.episodive.core.designsystem.component.EpisodesSection
import io.jacob.episodive.core.designsystem.component.EpisodiveSearchBar
import io.jacob.episodive.core.designsystem.component.PodcastsSection
import io.jacob.episodive.core.designsystem.component.StateImage
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.SearchResult
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestDataList

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
    onPodcastClick: (Podcast) -> Unit = {},
    onEpisodeClick: (Episode) -> Unit = {},
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
            val podcasts = searchResult.podcasts
            PodcastsSection(
                title = "Podcasts",
                podcasts = podcasts,
                onMore = { /* TODO */ },
                onPodcastClick = onPodcastClick
            )
        },
        contentOnExpand = { scrollState ->
            LazyColumn(state = scrollState) {
                items(
                    count = searchResult.podcasts.size,
                    key = { searchResult.podcasts[it].id },
                ) { index ->
                    val podcast = searchResult.podcasts[index]
                    ListItem(
                        headlineContent = { Text(podcast.title) },
                        supportingContent = { Text("podcast • ${podcast.author}") },
                        leadingContent = {
                            StateImage(
                                imageUrl = podcast.image,
                                contentDescription = podcast.title,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                        },
//                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .clickable {
                                onPodcastClick(podcast)
                            }
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
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
                        episode = episode,
                        onClick = { onEpisodeClick(episode) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    )
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
        )
    }
}