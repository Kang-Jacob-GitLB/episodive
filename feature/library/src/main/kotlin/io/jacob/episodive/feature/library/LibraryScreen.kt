package io.jacob.episodive.feature.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.mutableIntStateOf
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(top = systemBarsPadding.calculateTopPadding()),
    ) {
        item {
            Header(
                query = query,
                onQueryChange = onQueryChange,
                onFind = onFind
            )
        }

        item {
            PlayedEpisodeRowSection(
                title = stringResource(R.string.feature_library_section_recently_listened_episodes),
                playedEpisodes = playedEpisodes,
                onPlayedEpisodeClick = onPlayedEpisodeClick,
                onMore = { /* TODO */ },
            )
        }

        item {
            EpisodeRowSection(
                title = stringResource(R.string.feature_library_section_liked_episodes),
                episodes = likedEpisodes.map { it.episode },
                onEpisodeClick = onEpisodeClick,
                onMore = { /* TODO */ },
            )
        }

        item {
            PodcastsSection(
                title = stringResource(R.string.feature_library_section_followed_podcasts),
                podcasts = followedPodcasts.map { it.podcast },
                onPodcastClick = onPodcastClick,
                onMore = { /* TODO */ },
            )
        }

        item {
            CategorySection(
                title = stringResource(R.string.feature_library_section_preferred_categories),
                categories = preferredCategories,
                onCategoryClick = {},
                onMore = { /* TODO */ },
            )
        }

        item {
            Spacer(modifier = Modifier.height(LocalDimensionTheme.current.playerBarHeight))
        }
    }
}

@Composable
private fun Header(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onFind: (String) -> Unit,
) {
    var showFind by remember { mutableStateOf(false) }
    val items = listOf(
        stringResource(R.string.feature_library_filter_all),
        stringResource(R.string.feature_library_filter_recently_listened),
        stringResource(R.string.feature_library_filter_liked),
        stringResource(R.string.feature_library_filter_followed),
        stringResource(R.string.feature_library_filter_preferred),
    )
    var selectedIndex by remember { mutableIntStateOf(0) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
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
                    onClick = { showFind = true }
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

    AnimatedContent(
        targetState = showFind,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        },
        label = "search_content"
    ) { isShowingFind ->
        if (isShowingFind) {
            SearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                windowInsets = WindowInsets(0, 0, 0, 0),
                inputField = {
                    SearchBarDefaults.InputField(
                        query = query,
                        onQueryChange = onQueryChange,
                        onSearch = {
                            onFind(query)
                            showFind = false
                        },
                        expanded = false,
                        onExpandedChange = { if (!it) showFind = false },
                        placeholder = { Text(stringResource(R.string.feature_library_find_your_library)) },
                        leadingIcon = {
                            IconButton(onClick = { showFind = false }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                            }
                        }
                    )
                },
                expanded = false,
                onExpandedChange = { if (!it) showFind = false }
            ) {
                // 검색 결과
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(items) { index, label ->
                    EpisodiveFilterChip(
                        selected = selectedIndex == index,
                        onSelectedChange = { selectedIndex = index },
                        label = { Text(label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayedEpisodeRowSection(
    modifier: Modifier = Modifier,
    title: String,
    playedEpisodes: List<PlayedEpisode>,
    onPlayedEpisodeClick: (PlayedEpisode) -> Unit,
    onMore: () -> Unit,
) {
    SectionHeader(
        modifier = modifier,
        title = title,
        actionIcon = EpisodiveIcons.KeyboardArrowRight,
        actionIconContentDescription = title,
        onActionClick = onMore,
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
    onMore: () -> Unit,
) {
    SectionHeader(
        modifier = modifier,
        title = title,
        actionIcon = EpisodiveIcons.KeyboardArrowRight,
        actionIconContentDescription = title,
        onActionClick = onMore,
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
    onMore: () -> Unit,
) {
    SectionHeader(
        modifier = modifier,
        title = title,
        contentPadding = PaddingValues(horizontal = 16.dp),
        onActionClick = onMore
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
            playedEpisodes = playedEpisodeTestDataList,
            likedEpisodes = likedEpisodeTestDataList,
            followedPodcasts = followedPodcastTestDataList,
            preferredCategories = Category.entries
        )
    }
}