package io.jacob.episodive.feature.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import io.jacob.episodive.core.designsystem.component.EpisodiveFilterChip
import io.jacob.episodive.core.designsystem.component.EpisodiveScaffold
import io.jacob.episodive.core.designsystem.component.SectionHeader
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.screen.ErrorScreen
import io.jacob.episodive.core.designsystem.screen.LoadingScreen
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.SelectableCategory
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.ui.CategoryButton
import io.jacob.episodive.core.ui.CategoryItem
import io.jacob.episodive.core.ui.EpisodeDetailItem
import io.jacob.episodive.core.ui.EpisodeItem
import io.jacob.episodive.core.ui.PlayedEpisodeItem
import io.jacob.episodive.core.ui.PodcastDetailItem
import io.jacob.episodive.core.ui.PodcastsSection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf

@Composable
fun LibraryRoute(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    onPodcastClick: (Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is LibraryEffect.NavigateToPodcast -> onPodcastClick(effect.podcastId)
            }
        }
    }

    when (val s = state) {
        is LibraryState.Loading -> LoadingScreen()

        is LibraryState.Success -> LibraryScreen(
            modifier = modifier,
            query = s.findQuery,
            onQueryChange = { viewModel.sendAction(LibraryAction.QueryChanged(it)) },
            onFind = { viewModel.sendAction(LibraryAction.ClickFind(it)) },
            section = s.section,
            onSectionChange = { viewModel.sendAction(LibraryAction.SelectSection(it)) },
            playedEpisodes = s.allPlayedEpisodes,
            likedEpisodes = s.likedEpisodes,
            savedEpisodes = s.savedEpisodes,
            followedPodcasts = s.followedPodcasts,
            preferredCategories = s.preferredCategories,
            selectableCategories = s.selectableCategories,
            playedEpisodesPaging = viewModel.playedEpisodesPaging,
            likedEpisodesPaging = viewModel.likedEpisodesPaging,
            savedEpisodesPaging = viewModel.savedEpisodesPaging,
            followedPodcastsPaging = viewModel.followedPodcastsPaging,
            onPlayedEpisodeClick = { viewModel.sendAction(LibraryAction.ClickPlayingEpisode(it)) },
            onEpisodeClick = { viewModel.sendAction(LibraryAction.ClickEpisode(it)) },
            onPodcastClick = { viewModel.sendAction(LibraryAction.ClickPodcast(it)) },
            onToggleLikedEpisode = { viewModel.sendAction(LibraryAction.ToggleLikedEpisode(it)) },
            onToggleSavedEpisode = { viewModel.sendAction(LibraryAction.ToggleSavedEpisode(it)) },
            onToggleFollowedPodcast = { viewModel.sendAction(LibraryAction.ToggleFollowedPodcast(it)) },
            onTogglePreferredCategory = {
                viewModel.sendAction(
                    LibraryAction.TogglePreferredCategory(
                        it
                    )
                )
            },
        )

        is LibraryState.Error -> ErrorScreen(message = s.message)
    }
}

@Composable
internal fun LibraryScreen(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onFind: (String) -> Unit,
    section: LibrarySection,
    onSectionChange: (LibrarySection) -> Unit = {},
    playedEpisodes: List<Episode>,
    likedEpisodes: List<Episode>,
    savedEpisodes: List<Episode>,
    followedPodcasts: List<Podcast>,
    preferredCategories: List<Category>,
    selectableCategories: List<SelectableCategory>,
    playedEpisodesPaging: Flow<PagingData<SeparatedUiModel<Episode>>>,
    likedEpisodesPaging: Flow<PagingData<SeparatedUiModel<Episode>>>,
    savedEpisodesPaging: Flow<PagingData<SeparatedUiModel<Episode>>>,
    followedPodcastsPaging: Flow<PagingData<SeparatedUiModel<Podcast>>>,
    onPlayedEpisodeClick: (Episode) -> Unit = {},
    onEpisodeClick: (Episode) -> Unit = {},
    onPodcastClick: (Podcast) -> Unit = {},
    onToggleLikedEpisode: (Episode) -> Unit = {},
    onToggleSavedEpisode: (Episode) -> Unit = {},
    onToggleFollowedPodcast: (Podcast) -> Unit = {},
    onTogglePreferredCategory: (Category) -> Unit = {},
) {
    var showFind by remember { mutableStateOf(false) }
    val scrollState = rememberLazyListState()

    EpisodiveScaffold(
        modifier = modifier,
        title = stringResource(R.string.feature_library_title),
        subTitle = {
            FindOrFilter(
                scrollState = scrollState,
                showFind = showFind,
                onShowFindChanged = { showFind = it },
                query = query,
                onQueryChange = onQueryChange,
                onFind = onFind,
                section = section,
                onSectionChange = onSectionChange
            )
        },
        actionIcon = if (showFind) EpisodiveIcons.Close else EpisodiveIcons.Search,
        actionIconContentDescription = "search",
        onActionClick = {
            showFind = !showFind
            if (showFind) {
                onSectionChange(LibrarySection.All)
            }
        }
    ) { paddingValues, nestedScrollConnection ->
        when (section) {
            LibrarySection.All -> AllSectionContent(
                modifier = modifier,
                paddingValues = paddingValues,
                scrollState = scrollState,
                nestedScrollConnection = nestedScrollConnection,
                playedEpisodes = playedEpisodes,
                likedEpisodes = likedEpisodes,
                savedEpisodes = savedEpisodes,
                followedPodcasts = followedPodcasts,
                preferredCategories = preferredCategories,
                onPlayedEpisodeClick = onPlayedEpisodeClick,
                onEpisodeClick = onEpisodeClick,
                onPodcastClick = onPodcastClick
            )

            LibrarySection.RecentlyListened -> RecentlyListenedContent(
                modifier = modifier,
                paddingValues = paddingValues,
                nestedScrollConnection = nestedScrollConnection,
                playedEpisodesPaging = playedEpisodesPaging,
                onPlayedEpisodeClick = onPlayedEpisodeClick
            )

            LibrarySection.Liked -> LikedContent(
                modifier = modifier,
                paddingValues = paddingValues,
                nestedScrollConnection = nestedScrollConnection,
                likedEpisodesPaging = likedEpisodesPaging,
                onLikedEpisodeClick = { onEpisodeClick(it) },
                onToggleLiked = onToggleLikedEpisode
            )

            LibrarySection.Saved -> SavedContent(
                modifier = modifier,
                paddingValues = paddingValues,
                nestedScrollConnection = nestedScrollConnection,
                savedEpisodesPaging = savedEpisodesPaging,
                onSavedEpisodeClick = { onEpisodeClick(it) },
                onToggleSaved = onToggleSavedEpisode
            )

            LibrarySection.Followed -> FollowedContent(
                modifier = modifier,
                paddingValues = paddingValues,
                nestedScrollConnection = nestedScrollConnection,
                followedPodcastsPaging = followedPodcastsPaging,
                onFollowedPodcastClick = { onPodcastClick(it) },
                onToggleFollowed = onToggleFollowedPodcast
            )

            LibrarySection.Preferred -> PreferredContent(
                modifier = modifier,
                paddingValues = paddingValues,
                nestedScrollConnection = nestedScrollConnection,
                selectableCategories = selectableCategories,
                onCategoryClick = {},
                onTogglePreferred = onTogglePreferredCategory
            )
        }
    }
}

@Composable
private fun AllSectionContent(
    modifier: Modifier = Modifier,
    scrollState: LazyListState,
    paddingValues: PaddingValues,
    nestedScrollConnection: NestedScrollConnection,
    playedEpisodes: List<Episode>,
    likedEpisodes: List<Episode>,
    savedEpisodes: List<Episode>,
    followedPodcasts: List<Podcast>,
    preferredCategories: List<Category>,
    onPlayedEpisodeClick: (Episode) -> Unit,
    onEpisodeClick: (Episode) -> Unit,
    onPodcastClick: (Podcast) -> Unit,
) {
    LazyColumn(
        modifier = modifier
            .padding(paddingValues)
            .nestedScroll(nestedScrollConnection),
        state = scrollState,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (playedEpisodes.isNotEmpty()) {
                    PlayedEpisodeRowSection(
                        title = stringResource(R.string.feature_library_section_recently_listened_episodes),
                        playedEpisodes = playedEpisodes,
                        onPlayedEpisodeClick = onPlayedEpisodeClick,
                    )
                }

                if (likedEpisodes.isNotEmpty()) {
                    EpisodeRowSection(
                        title = stringResource(R.string.feature_library_section_liked_episodes),
                        episodes = likedEpisodes,
                        onEpisodeClick = onEpisodeClick,
                    )
                }

                if (savedEpisodes.isNotEmpty()) {
                    EpisodeRowSection(
                        title = stringResource(R.string.feature_library_section_saved_episodes),
                        episodes = savedEpisodes,
                        onEpisodeClick = onEpisodeClick,
                    )
                }

                if (followedPodcasts.isNotEmpty()) {
                    PodcastsSection(
                        title = stringResource(R.string.feature_library_section_followed_podcasts),
                        podcasts = followedPodcasts,
                        onPodcastClick = onPodcastClick,
                    )
                }

                if (preferredCategories.isNotEmpty()) {
                    CategorySection(
                        title = stringResource(R.string.feature_library_section_preferred_categories),
                        categories = preferredCategories,
                        onCategoryClick = {},
                    )
                }

                if (
                    playedEpisodes.isEmpty() &&
                    likedEpisodes.isEmpty() &&
                    savedEpisodes.isEmpty() &&
                    followedPodcasts.isEmpty() &&
                    preferredCategories.isEmpty()
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        text = stringResource(R.string.feature_library_not_found_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(LocalDimensionTheme.current.playerBarHeight))
        }
    }
}

@Composable
private fun RecentlyListenedContent(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    nestedScrollConnection: NestedScrollConnection,
    playedEpisodesPaging: Flow<PagingData<SeparatedUiModel<Episode>>>,
    onPlayedEpisodeClick: (Episode) -> Unit,
) {
    val items = playedEpisodesPaging.collectAsLazyPagingItems()

    LazyColumn(
        modifier = modifier
            .padding(paddingValues)
            .nestedScroll(nestedScrollConnection),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            count = items.itemCount,
            key = items.itemKey {
                when (it) {
                    is SeparatedUiModel.Content -> it.data.id
                    is SeparatedUiModel.Separator -> it.label
                }
            },
            contentType = {
                when (items[it]) {
                    is SeparatedUiModel.Content -> "episode"
                    is SeparatedUiModel.Separator -> "separator"
                    null -> "loading"
                }
            }
        ) { index ->
            when (val item = items[index] ?: return@items) {
                is SeparatedUiModel.Content -> {
                    val episode = item.data
                    PlayedEpisodeItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                        playedEpisode = episode,
                        onClick = { onPlayedEpisodeClick(episode) },
                    )
                }

                is SeparatedUiModel.Separator -> {
                    val date = item.label
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp)
                            .padding(vertical = 8.dp),
                        text = date,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(LocalDimensionTheme.current.playerBarHeight))
        }
    }
}

@Composable
private fun LikedContent(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    nestedScrollConnection: NestedScrollConnection,
    likedEpisodesPaging: Flow<PagingData<SeparatedUiModel<Episode>>>,
    onLikedEpisodeClick: (Episode) -> Unit,
    onToggleLiked: (Episode) -> Unit,
) {
    val items = likedEpisodesPaging.collectAsLazyPagingItems()

    LazyColumn(
        modifier = modifier
            .padding(paddingValues)
            .nestedScroll(nestedScrollConnection),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            count = items.itemCount,
            key = items.itemKey {
                when (it) {
                    is SeparatedUiModel.Content -> it.data.id
                    is SeparatedUiModel.Separator -> it.label
                }
            },
            contentType = {
                when (items[it]) {
                    is SeparatedUiModel.Content -> "episode"
                    is SeparatedUiModel.Separator -> "separator"
                    null -> "loading"
                }
            }
        ) { index ->
            when (val item = items[index] ?: return@items) {
                is SeparatedUiModel.Content -> {
                    val episode = item.data
                    EpisodeItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                        episode = episode,
                        onClick = { onLikedEpisodeClick(episode) },
                        onToggleLiked = { onToggleLiked(episode) }
                    )
                }

                is SeparatedUiModel.Separator -> {
                    val date = item.label
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp)
                            .padding(vertical = 8.dp),
                        text = date,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(LocalDimensionTheme.current.playerBarHeight))
        }
    }
}

@Composable
private fun SavedContent(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    nestedScrollConnection: NestedScrollConnection,
    savedEpisodesPaging: Flow<PagingData<SeparatedUiModel<Episode>>>,
    onSavedEpisodeClick: (Episode) -> Unit,
    onToggleSaved: (Episode) -> Unit,
) {
    val items = savedEpisodesPaging.collectAsLazyPagingItems()

    LazyColumn(
        modifier = modifier
            .padding(paddingValues)
            .nestedScroll(nestedScrollConnection),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            count = items.itemCount,
            key = items.itemKey {
                when (it) {
                    is SeparatedUiModel.Content -> it.data.id
                    is SeparatedUiModel.Separator -> it.label
                }
            },
            contentType = {
                when (items[it]) {
                    is SeparatedUiModel.Content -> "episode"
                    is SeparatedUiModel.Separator -> "separator"
                    null -> "loading"
                }
            }
        ) { index ->
            when (val item = items[index] ?: return@items) {
                is SeparatedUiModel.Content -> {
                    val episode = item.data
                    EpisodeItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                        episode = episode,
                        onClick = { onSavedEpisodeClick(episode) },
                        onToggleLiked = {},
                        onToggleSaved = { onToggleSaved(episode) }
                    )
                }

                is SeparatedUiModel.Separator -> {
                    val date = item.label
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp)
                            .padding(vertical = 8.dp),
                        text = date,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(LocalDimensionTheme.current.playerBarHeight))
        }
    }
}

@Composable
private fun FollowedContent(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    nestedScrollConnection: NestedScrollConnection,
    followedPodcastsPaging: Flow<PagingData<SeparatedUiModel<Podcast>>>,
    onFollowedPodcastClick: (Podcast) -> Unit,
    onToggleFollowed: (Podcast) -> Unit,
) {
    val items = followedPodcastsPaging.collectAsLazyPagingItems()

    LazyColumn(
        modifier = modifier
            .padding(paddingValues)
            .nestedScroll(nestedScrollConnection),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            count = items.itemCount,
            key = items.itemKey {
                when (it) {
                    is SeparatedUiModel.Content -> it.data.id
                    is SeparatedUiModel.Separator -> it.label
                }
            },
            contentType = {
                when (items[it]) {
                    is SeparatedUiModel.Content -> "podcast"
                    is SeparatedUiModel.Separator -> "separator"
                    null -> "loading"
                }
            }
        ) { index ->
            when (val item = items[index] ?: return@items) {
                is SeparatedUiModel.Content -> {
                    val podcast = item.data
                    PodcastDetailItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                        podcast = podcast,
                        onClick = { onFollowedPodcastClick(podcast) },
                        onToggleFollowed = { onToggleFollowed(podcast) }
                    )
                }

                is SeparatedUiModel.Separator -> {
                    val date = item.label
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp)
                            .padding(vertical = 8.dp),
                        text = date,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

        }

        item {
            Spacer(modifier = Modifier.height(LocalDimensionTheme.current.playerBarHeight))
        }
    }
}

@Composable
private fun PreferredContent(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    nestedScrollConnection: NestedScrollConnection,
    selectableCategories: List<SelectableCategory>,
    onCategoryClick: (Category) -> Unit = {},
    onTogglePreferred: (Category) -> Unit = {},
) {
    LazyVerticalGrid(
        modifier = modifier
            .padding(paddingValues)
            .nestedScroll(nestedScrollConnection),
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        items(
            items = selectableCategories,
            key = { it.category.id },
        ) {
            val category = it.category
            CategoryButton(
                modifier = Modifier
                    .aspectRatio(1f)
                    .animateItem(),
                category = category,
                isSelected = it.isSelected,
                onClick = { onTogglePreferred(category) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(LocalDimensionTheme.current.playerBarHeight))
        }
    }
}

@Composable
private fun FindBar(
    modifier: Modifier = Modifier,
    scrollState: LazyListState,
    query: String,
    onQueryChange: (String) -> Unit,
    onFind: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            keyboardController?.hide()
        }
    }

    SearchBar(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        windowInsets = WindowInsets(0, 0, 0, 0),
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = {
                    onFind(query)
                    keyboardController?.hide()
                },
                expanded = false,
                onExpandedChange = { if (!it) onDismiss() },
                placeholder = { Text(stringResource(R.string.feature_library_find_your_library)) },
                leadingIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(EpisodiveIcons.Search, null)
                    }
                }
            )
        },
        expanded = false,
        onExpandedChange = { if (!it) onDismiss() },
        content = {}
    )
}

@Composable
private fun SectionFilter(
    modifier: Modifier = Modifier,
    currentSection: LibrarySection,
    onSectionChange: (LibrarySection) -> Unit,
) {
    val sectionNames = mapOf(
        LibrarySection.All to stringResource(R.string.feature_library_filter_all),
        LibrarySection.RecentlyListened to stringResource(R.string.feature_library_filter_recently_listened),
        LibrarySection.Liked to stringResource(R.string.feature_library_filter_liked),
        LibrarySection.Saved to stringResource(R.string.feature_library_filter_saved),
        LibrarySection.Followed to stringResource(R.string.feature_library_filter_followed),
        LibrarySection.Preferred to stringResource(R.string.feature_library_filter_preferred),
    )

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = LibrarySection.entries,
            key = { it.name },
        ) { section ->
            EpisodiveFilterChip(
                selected = currentSection == section,
                onSelectedChange = { if (it) onSectionChange(section) },
                label = { Text(sectionNames[section] ?: "") },
            )
        }
    }
}

@Composable
private fun FindOrFilter(
    modifier: Modifier = Modifier,
    scrollState: LazyListState,
    showFind: Boolean,
    onShowFindChanged: (Boolean) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    onFind: (String) -> Unit,
    section: LibrarySection,
    onSectionChange: (LibrarySection) -> Unit,
) {
    AnimatedContent(
        modifier = modifier,
        targetState = showFind,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        },
        label = "search_content"
    ) { isShowingFind ->
        if (isShowingFind) {
            FindBar(
                scrollState = scrollState,
                query = query,
                onQueryChange = onQueryChange,
                onFind = onFind,
                onDismiss = { onShowFindChanged(false) },
            )
        } else {
            SectionFilter(
                currentSection = section,
                onSectionChange = onSectionChange
            )
        }
    }
}

@Composable
private fun PlayedEpisodeRowSection(
    modifier: Modifier = Modifier,
    title: String,
    playedEpisodes: List<Episode>,
    onPlayedEpisodeClick: (Episode) -> Unit,
) {
    SectionHeader(
        modifier = modifier,
        title = title,
    ) {
        val lazyListState = rememberLazyListState()
        val flingBehavior = rememberSnapFlingBehavior(
            lazyListState = lazyListState,
            snapPosition = SnapPosition.Start,
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            state = lazyListState,
            flingBehavior = flingBehavior,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(
                count = playedEpisodes.size,
                key = { playedEpisodes[it].id },
            ) {
                PlayedEpisodeItem(
                    modifier = Modifier.width(250.dp),
                    playedEpisode = playedEpisodes[it],
                    onClick = { onPlayedEpisodeClick(playedEpisodes[it]) }
                )
            }
        }
    }
}

@Composable
private fun EpisodeRowSection(
    modifier: Modifier = Modifier,
    title: String,
    episodes: List<Episode>,
    onEpisodeClick: (Episode) -> Unit,
) {
    SectionHeader(
        modifier = modifier,
        title = title,
    ) {
        val lazyListState = rememberLazyListState()
        val flingBehavior = rememberSnapFlingBehavior(
            lazyListState = lazyListState,
            snapPosition = SnapPosition.Start,
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            state = lazyListState,
            flingBehavior = flingBehavior,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(
                count = episodes.size,
                key = { episodes[it].id },
            ) {
                EpisodeDetailItem(
                    episode = episodes[it],
                    onClick = { onEpisodeClick(episodes[it]) }
                )
            }
        }
    }
}

@Composable
private fun CategorySection(
    modifier: Modifier = Modifier,
    title: String,
    categories: List<Category>,
    onCategoryClick: (Category) -> Unit,
) {
    SectionHeader(
        modifier = modifier,
        title = title,
    ) {
        val lazyListState = rememberLazyListState()
        val flingBehavior = rememberSnapFlingBehavior(
            lazyListState = lazyListState,
            snapPosition = SnapPosition.Start,
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            state = lazyListState,
            flingBehavior = flingBehavior,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(
                items = categories,
                key = { it.id },
            ) { category ->
                CategoryItem(
                    category = category,
                    onClick = { onCategoryClick(category) }
                )
            }
        }
    }
}

@DevicePreviews
@Composable
private fun LibraryScreenPreview() {
    EpisodiveTheme {
        LibraryScreen(
            query = "test",
            onQueryChange = {},
            onFind = {},
            section = LibrarySection.All,
            playedEpisodes = episodeTestDataList,
            likedEpisodes = episodeTestDataList,
            savedEpisodes = episodeTestDataList,
            followedPodcasts = podcastTestDataList,
            preferredCategories = Category.entries,
            playedEpisodesPaging = flowOf(PagingData.from(episodeTestDataList.map {
                SeparatedUiModel.Content(it)
            })),
            likedEpisodesPaging = flowOf(PagingData.from(episodeTestDataList.map {
                SeparatedUiModel.Content(it)
            })),
            savedEpisodesPaging = flowOf(PagingData.from(episodeTestDataList.map {
                SeparatedUiModel.Content(it)
            })),
            followedPodcastsPaging = flowOf(PagingData.from(podcastTestDataList.map {
                SeparatedUiModel.Content(it)
            })),
            selectableCategories = Category.entries.map { category ->
                SelectableCategory(
                    category = category,
                    isSelected = true,
                )
            },
        )
    }
}