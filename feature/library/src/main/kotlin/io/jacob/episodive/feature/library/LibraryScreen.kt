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
import io.jacob.episodive.core.designsystem.component.CategoryButton
import io.jacob.episodive.core.designsystem.component.CategoryItem
import io.jacob.episodive.core.designsystem.component.EpisodeDetailItem
import io.jacob.episodive.core.designsystem.component.EpisodeItem
import io.jacob.episodive.core.designsystem.component.EpisodiveFilterChip
import io.jacob.episodive.core.designsystem.component.EpisodiveScaffold
import io.jacob.episodive.core.designsystem.component.PlayedEpisodeItem
import io.jacob.episodive.core.designsystem.component.PodcastDetailItem
import io.jacob.episodive.core.designsystem.component.PodcastsSection
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
import io.jacob.episodive.core.model.mapper.toHumanReadable
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestDataList
import kotlinx.coroutines.flow.collectLatest

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
                is LibraryEffect.NavigateToPodcast -> onPodcastClick(effect.podcast.id)
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
            followedPodcasts = s.followedPodcasts,
            preferredCategories = s.preferredCategories,
            selectableCategories = s.selectableCategories,
            onPlayedEpisodeClick = { viewModel.sendAction(LibraryAction.ClickPlayingEpisode(it)) },
            onEpisodeClick = { viewModel.sendAction(LibraryAction.ClickEpisode(it)) },
            onPodcastClick = { viewModel.sendAction(LibraryAction.ClickPodcast(it)) },
            onToggleLikedEpisode = { viewModel.sendAction(LibraryAction.ToggleLikedEpisode(it)) },
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
private fun LibraryScreen(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onFind: (String) -> Unit,
    section: LibrarySection,
    onSectionChange: (LibrarySection) -> Unit = {},
    playedEpisodes: List<Episode>,
    likedEpisodes: List<Episode>,
    followedPodcasts: List<Podcast>,
    preferredCategories: List<Category>,
    selectableCategories: List<SelectableCategory>,
    onPlayedEpisodeClick: (Episode) -> Unit = {},
    onEpisodeClick: (Episode) -> Unit = {},
    onPodcastClick: (Podcast) -> Unit = {},
    onToggleLikedEpisode: (Episode) -> Unit = {},
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
                playedEpisodes = playedEpisodes,
                onPlayedEpisodeClick = onPlayedEpisodeClick
            )

            LibrarySection.Liked -> LikedContent(
                modifier = modifier,
                paddingValues = paddingValues,
                nestedScrollConnection = nestedScrollConnection,
                likedEpisodes = likedEpisodes,
                onLikedEpisodeClick = { onEpisodeClick(it) },
                onToggleLiked = onToggleLikedEpisode
            )

            LibrarySection.Followed -> FollowedContent(
                modifier = modifier,
                paddingValues = paddingValues,
                nestedScrollConnection = nestedScrollConnection,
                followedPodcasts = followedPodcasts,
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
    playedEpisodes: List<Episode>,
    onPlayedEpisodeClick: (Episode) -> Unit,
) {
    val groupedEpisodes = remember(playedEpisodes) {
        playedEpisodes
            .filter { it.playedAt != null }
            .groupBy { it.playedAt!!.toHumanReadable() }
    }

    LazyColumn(
        modifier = modifier
            .padding(paddingValues)
            .nestedScroll(nestedScrollConnection),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedEpisodes.forEach { (dateLabel, episodes) ->
            stickyHeader(
                key = "header_$dateLabel",
                contentType = "date_header"
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp)
                        .padding(vertical = 8.dp),
                    text = dateLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(
                items = episodes,
                key = { it.id },
                contentType = { "episode" }
            ) { playedEpisode ->
                PlayedEpisodeItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                    playedEpisode = playedEpisode,
                    onClick = { onPlayedEpisodeClick(playedEpisode) },
                )
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
    likedEpisodes: List<Episode>,
    onLikedEpisodeClick: (Episode) -> Unit,
    onToggleLiked: (Episode) -> Unit,
) {
    val groupedEpisodes = remember(likedEpisodes) {
        likedEpisodes
            .filter { it.likedAt != null }
            .groupBy { it.likedAt!!.toHumanReadable() }
    }

    LazyColumn(
        modifier = modifier
            .padding(paddingValues)
            .nestedScroll(nestedScrollConnection),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedEpisodes.forEach { (dateLabel, episodes) ->
            stickyHeader(
                key = "header_$dateLabel",
                contentType = "date_header"
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp)
                        .padding(vertical = 8.dp),
                    text = dateLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(
                items = episodes,
                key = { it.id },
                contentType = { "episode" }
            ) { likedEpisode ->
                EpisodeItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                    episode = likedEpisode,
                    onClick = { onLikedEpisodeClick(likedEpisode) },
                    onToggleLiked = { onToggleLiked(likedEpisode) }
                )
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
    followedPodcasts: List<Podcast>,
    onFollowedPodcastClick: (Podcast) -> Unit,
    onToggleFollowed: (Podcast) -> Unit,
) {
    val groupedPodcasts = remember(followedPodcasts) {
        followedPodcasts
            .filter { it.followedAt != null }
            .groupBy { it.followedAt!!.toHumanReadable() }
    }

    LazyColumn(
        modifier = modifier
            .padding(paddingValues)
            .nestedScroll(nestedScrollConnection),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedPodcasts.forEach { (dateLabel, podcasts) ->
            stickyHeader(
                key = "header_$dateLabel",
                contentType = "date_header"
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp)
                        .padding(vertical = 8.dp),
                    text = dateLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(
                items = podcasts,
                key = { it.id },
                contentType = { "podcast" }
            ) { followedPodcast ->
                PodcastDetailItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                    podcast = followedPodcast,
                    onClick = { onFollowedPodcastClick(followedPodcast) },
                    onToggleFollowed = { onToggleFollowed(followedPodcast) }
                )
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
            followedPodcasts = podcastTestDataList,
            preferredCategories = Category.entries,
            selectableCategories = Category.entries.map { category ->
                SelectableCategory(
                    category = category,
                    isSelected = true,
                )
            },
        )
    }
}