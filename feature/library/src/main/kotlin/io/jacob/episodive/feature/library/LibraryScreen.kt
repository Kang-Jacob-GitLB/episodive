package io.jacob.episodive.feature.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.jacob.episodive.core.designsystem.component.EpisodeDetailItem
import io.jacob.episodive.core.designsystem.component.EpisodiveFilterChip
import io.jacob.episodive.core.designsystem.component.PlayedEpisodeItem
import io.jacob.episodive.core.designsystem.component.PodcastsSection
import io.jacob.episodive.core.designsystem.component.SectionHeader
import io.jacob.episodive.core.designsystem.component.StateImage
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.screen.ErrorScreen
import io.jacob.episodive.core.designsystem.screen.LoadingScreen
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.FollowedPodcast
import io.jacob.episodive.core.model.LibraryFindResult
import io.jacob.episodive.core.model.LikedEpisode
import io.jacob.episodive.core.model.PlayedEpisode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.testing.model.followedPodcastTestDataList
import io.jacob.episodive.core.testing.model.likedEpisodeTestDataList
import io.jacob.episodive.core.testing.model.playedEpisodeTestDataList
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
            findResult = s.findResult,
            section = s.section,
            onSectionChange = { viewModel.sendAction(LibraryAction.SelectSection(it)) },
            playedEpisodes = s.allPlayedEpisodes,
            likedEpisodes = s.likedEpisodes,
            followedPodcasts = s.followedPodcasts,
            preferredCategories = s.preferredCategories,
            onPlayedEpisodeClick = { viewModel.sendAction(LibraryAction.ClickPlayingEpisode(it)) },
            onEpisodeClick = { viewModel.sendAction(LibraryAction.ClickEpisode(it)) },
            onPodcastClick = { viewModel.sendAction(LibraryAction.ClickPodcast(it)) },
            onToggleLikedEpisode = { viewModel.sendAction(LibraryAction.ToggleLikedEpisode(it)) },
            onToggleFollowedPodcast = { viewModel.sendAction(LibraryAction.ToggleFollowedPodcast(it)) }
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
    findResult: LibraryFindResult,
    section: LibrarySection,
    onSectionChange: (LibrarySection) -> Unit = {},
    playedEpisodes: List<PlayedEpisode>,
    likedEpisodes: List<LikedEpisode>,
    followedPodcasts: List<FollowedPodcast>,
    preferredCategories: List<Category>,
    onPlayedEpisodeClick: (PlayedEpisode) -> Unit = {},
    onEpisodeClick: (Episode) -> Unit = {},
    onPodcastClick: (Podcast) -> Unit = {},
    onToggleLikedEpisode: (LikedEpisode) -> Unit = {},
    onToggleFollowedPodcast: (FollowedPodcast) -> Unit = {},
) {
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

    var showFind by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(top = systemBarsPadding.calculateTopPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            LibraryHeader(
                showFind = showFind,
                onShowFindChanged = { showFind = it }
            )
        }

        stickyHeader {
            FindOrFilter(
                showFind = showFind,
                onShowFindChanged = { showFind = it },
                query = query,
                onQueryChange = onQueryChange,
                onFind = onFind,
                section = section,
                onSectionChange = onSectionChange
            )
        }

        if (section == LibrarySection.All) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PlayedEpisodeRowSection(
                        title = stringResource(R.string.feature_library_section_recently_listened_episodes),
                        playedEpisodes = playedEpisodes,
                        onPlayedEpisodeClick = onPlayedEpisodeClick,
                    )

                    EpisodeRowSection(
                        title = stringResource(R.string.feature_library_section_liked_episodes),
                        episodes = likedEpisodes.map { it.episode },
                        onEpisodeClick = onEpisodeClick,
                    )

                    PodcastsSection(
                        title = stringResource(R.string.feature_library_section_followed_podcasts),
                        podcasts = followedPodcasts.map { it.podcast },
                        onPodcastClick = onPodcastClick,
                    )

                    CategorySection(
                        title = stringResource(R.string.feature_library_section_preferred_categories),
                        categories = preferredCategories,
                        onCategoryClick = {},
                    )
                }
            }
        }

        if (section == LibrarySection.RecentlyListened) {
            items(
                count = playedEpisodes.size,
                key = { index -> playedEpisodes[index].episode.id },
                contentType = { index -> playedEpisodes[index].episode },
            ) { index ->
                val playedEpisode = playedEpisodes[index]
                PlayedEpisodeItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
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
private fun LibraryHeader(
    modifier: Modifier = Modifier,
    showFind: Boolean,
    onShowFindChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.feature_library_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        AnimatedContent(
            targetState = showFind,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "search_icon"
        ) { isShowingFind ->
            if (!isShowingFind) {
                IconButton(
                    onClick = { onShowFindChanged(true) }
                ) {
                    Icon(
                        imageVector = EpisodiveIcons.SearchBorder,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = "search"
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }


}

@Composable
private fun FindOrFilter(
    modifier: Modifier = Modifier,
    showFind: Boolean,
    onShowFindChanged: (Boolean) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    onFind: (String) -> Unit,
    section: LibrarySection,
    onSectionChange: (LibrarySection) -> Unit,
) {
    AnimatedContent(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background),
        targetState = showFind,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        },
        label = "search_content"
    ) { isShowingFind ->
        if (isShowingFind) {
            FindBar(
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
private fun FindBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onFind: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    SearchBar(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        windowInsets = WindowInsets(0, 0, 0, 0),
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = {
                    onFind(query)
                    onDismiss()
                },
                expanded = false,
                onExpandedChange = { if (!it) onDismiss() },
                placeholder = { Text(stringResource(R.string.feature_library_find_your_library)) },
                leadingIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        expanded = false,
        onExpandedChange = { if (!it) onDismiss() }
    ) {
        // 검색 결과
    }
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
private fun PlayedEpisodeRowSection(
    modifier: Modifier = Modifier,
    title: String,
    playedEpisodes: List<PlayedEpisode>,
    onPlayedEpisodeClick: (PlayedEpisode) -> Unit,
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
                key = { playedEpisodes[it].episode.id },
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
        contentPadding = PaddingValues(horizontal = 16.dp),
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
        ) {
            items(
                count = categories.size,
                key = { categories[it].id },
            ) {
                CategoryItem(
                    category = categories[it],
                    onClick = { onCategoryClick(categories[it]) }
                )
            }
        }
    }
}

@Composable
private fun CategoryItem(
    modifier: Modifier = Modifier,
    category: Category,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clickable { onClick() },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StateImage(
            modifier = Modifier
                .size(140.dp)
                .clip(MaterialTheme.shapes.largeIncreased),
            imageUrl = category.imageUrl,
            contentDescription = category.name,
        )

        Text(
            text = category.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
            findResult = LibraryFindResult(),
            section = LibrarySection.All,
            playedEpisodes = playedEpisodeTestDataList,
            likedEpisodes = likedEpisodeTestDataList,
            followedPodcasts = followedPodcastTestDataList,
            preferredCategories = Category.entries
        )
    }
}