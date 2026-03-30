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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.jacob.episodive.core.designsystem.component.EpisodiveScaffold
import io.jacob.episodive.core.designsystem.component.EpisodiveSearchBar
import io.jacob.episodive.core.designsystem.component.SectionHeader
import io.jacob.episodive.core.designsystem.component.StateImage
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
import io.jacob.episodive.core.model.RecentSearch
import io.jacob.episodive.core.model.SearchResult
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.ui.EpisodeItem
import io.jacob.episodive.core.ui.EpisodesSection
import io.jacob.episodive.core.ui.PodcastsSection
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
                onToggleLikedEpisode = { viewModel.sendAction(SearchAction.ToggleLikedEpisode(it)) },
                onRecentSearchClick = { viewModel.sendAction(SearchAction.ClickRecentSearch(it)) },
                onRemoveRecentSearch = { viewModel.sendAction(SearchAction.RemoveRecentSearch(it)) },
                onClearRecentSearches = { viewModel.sendAction(SearchAction.ClearRecentSearches) },
            )
        }

        is SearchState.Error -> ErrorScreen(message = s.message)
    }
}

@Composable
internal fun SearchScreen(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    recentSearches: List<RecentSearch>,
    searchResult: SearchResult,
    podcasts: List<Podcast>,
    episodes: List<Episode>,
    onPodcastClick: (Podcast) -> Unit = {},
    onEpisodeClick: (Episode) -> Unit = {},
    onToggleLikedEpisode: (Episode) -> Unit = {},
    onRecentSearchClick: (RecentSearch) -> Unit = {},
    onRemoveRecentSearch: (RecentSearch) -> Unit = {},
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
                    onToggleLikedEpisode = onToggleLikedEpisode,
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
                    onToggleLikedEpisode = onToggleLikedEpisode,
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
    onToggleLikedEpisode: (Episode) -> Unit = {},
    onPodcastClick: (Podcast) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        if (podcasts.isNotEmpty()) {
            item {
                PodcastsSection(
                    modifier = Modifier
                        .fillMaxWidth(),
                    title = stringResource(R.string.feature_search_section_global_trending_feeds),
                    podcasts = podcasts,
                    subtitleProvider = { it.ownerName.ifEmpty { it.author } },
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
                    onToggleLikedEpisode = onToggleLikedEpisode,
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
    recentSearches: List<RecentSearch>,
    searchResult: SearchResult,
    onPodcastClick: (Podcast) -> Unit = {},
    onEpisodeClick: (Episode) -> Unit = {},
    onToggleLikedEpisode: (Episode) -> Unit = {},
    onRecentSearchClick: (RecentSearch) -> Unit = {},
    onRemoveRecentSearch: (RecentSearch) -> Unit = {},
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
                    SectionHeader(
                        modifier = Modifier
                            .fillMaxWidth(),
                        title = stringResource(R.string.feature_search_section_episodes),
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
                        onToggleLiked = { onToggleLikedEpisode(episode) }
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
    recentSearches: List<RecentSearch>,
    onRecentSearchClicked: (RecentSearch) -> Unit,
    onRemoveRecentSearch: (RecentSearch) -> Unit,
    onClearRecentSearches: () -> Unit
) {
    SectionHeader(
        modifier = modifier,
        title = title,
        actionIcon = EpisodiveIcons.PlaylistX,
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
    recentSearch: RecentSearch,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (recentSearch) {
            is RecentSearch.Query -> {
                Icon(
                    imageVector = EpisodiveIcons.History,
                    contentDescription = "Recent Search Icon",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = recentSearch.query,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            is RecentSearch.PodcastSearch -> {
                StateImage(
                    imageUrl = recentSearch.imageUrl,
                    contentDescription = recentSearch.title,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recentSearch.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (recentSearch.author.isNotEmpty()) {
                            stringResource(R.string.feature_search_recent_podcast_subtitle, recentSearch.author)
                        } else {
                            stringResource(R.string.feature_search_recent_podcast_subtitle_no_author)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            is RecentSearch.EpisodeSearch -> {
                StateImage(
                    imageUrl = recentSearch.imageUrl,
                    contentDescription = recentSearch.title,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recentSearch.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (recentSearch.feedTitle.isNotEmpty()) {
                            stringResource(R.string.feature_search_recent_episode_subtitle, recentSearch.feedTitle)
                        } else {
                            stringResource(R.string.feature_search_recent_episode_subtitle_no_feed)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

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
            recentSearches = emptyList(),
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
            recentSearches = emptyList(),
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
            recentSearches = listOf(
                RecentSearch.Query(
                    id = 1,
                    query = "개발 팟캐스트",
                    searchedAt = kotlin.time.Clock.System.now(),
                ),
                RecentSearch.PodcastSearch(
                    id = 2,
                    podcastId = 100,
                    title = "코틀린 라디오",
                    imageUrl = "",
                    author = "JetBrains",
                    searchedAt = kotlin.time.Clock.System.now(),
                ),
                RecentSearch.EpisodeSearch(
                    id = 3,
                    episodeId = 200,
                    title = "Compose UI 완전 정복 #42",
                    imageUrl = "",
                    feedTitle = "Android Developers",
                    searchedAt = kotlin.time.Clock.System.now(),
                ),
                RecentSearch.Query(
                    id = 4,
                    query = "machine learning",
                    searchedAt = kotlin.time.Clock.System.now(),
                ),
                RecentSearch.PodcastSearch(
                    id = 5,
                    podcastId = 101,
                    title = "The Changelog",
                    imageUrl = "",
                    author = "Changelog Media",
                    searchedAt = kotlin.time.Clock.System.now(),
                ),
            ),
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