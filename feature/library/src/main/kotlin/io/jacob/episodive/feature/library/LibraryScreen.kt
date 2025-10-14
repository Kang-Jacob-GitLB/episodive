package io.jacob.episodive.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.jacob.episodive.core.designsystem.component.EpisodesSection
import io.jacob.episodive.core.designsystem.component.EpisodiveFilterChip
import io.jacob.episodive.core.designsystem.component.PlayingEpisodesSection
import io.jacob.episodive.core.designsystem.component.PodcastsSection
import io.jacob.episodive.core.designsystem.component.SectionHeader
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.screen.ErrorScreen
import io.jacob.episodive.core.designsystem.screen.LoadingScreen
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.FollowedPodcast
import io.jacob.episodive.core.model.LibraryFindResult
import io.jacob.episodive.core.model.LikedEpisode
import io.jacob.episodive.core.model.PlayedEpisode
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
            playingEpisodes = s.playingEpisodes,
            likedEpisodes = s.likedEpisodes,
            followedPodcasts = s.followedPodcasts,
            onPlayedEpisodeClick = { viewModel.sendAction(LibraryAction.ClickPlayingEpisode(it)) },
            onLikedEpisodeClick = { viewModel.sendAction(LibraryAction.ClickLikedEpisode(it)) },
            onFollowedPodcastClick = { viewModel.sendAction(LibraryAction.ClickFollowedPodcast(it)) },
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
    playingEpisodes: List<PlayedEpisode>,
    likedEpisodes: List<LikedEpisode>,
    followedPodcasts: List<FollowedPodcast>,
    onPlayedEpisodeClick: (PlayedEpisode) -> Unit = {},
    onLikedEpisodeClick: (LikedEpisode) -> Unit = {},
    onFollowedPodcastClick: (FollowedPodcast) -> Unit = {},
    onToggleLikedEpisode: (LikedEpisode) -> Unit = {},
    onToggleFollowedPodcast: (FollowedPodcast) -> Unit = {},
) {
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(systemBarsPadding),
    ) {
        item {
            Header(
                query = query,
                onQueryChange = onQueryChange,
                onFind = onFind
            )
        }

        item {
            PlayingEpisodesSection(
                playingEpisodes = playingEpisodes,
                onEpisodeClick = onPlayedEpisodeClick
            )
        }

        item {
            EpisodesSection(
                title = "Liked Episodes",
                episodes = likedEpisodes.map { it.episode },
                onEpisodeClick = {
                    // TODO
                }
            )
        }

        item {
            PodcastsSection(
                title = "Followed Podcasts",
                podcasts = followedPodcasts.map { it.podcast },
                onMore = {},
                onPodcastClick = {
//                    onFollowedPodcastClick(it)
                }
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
        "All", "Recently listen", "Liked", "Followed"
    )
    var selectedIndex by remember { mutableIntStateOf(0) }

    SectionHeader(
        modifier = modifier,
        title = "내 라이브러리",
        actionIcon = if (showFind) null else EpisodiveIcons.SearchBorder,
        actionIconContentDescription = "검색",
        onActionClick = { showFind = true }
    ) {
        if (showFind) {
            SearchBar(
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
                        placeholder = { Text("검색") },
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

@DevicePreviews
@Composable
private fun LibraryScreenPreview() {
    EpisodiveTheme {
        LibraryScreen(
            query = "test",
            onQueryChange = {},
            onFind = {},
            findResult = LibraryFindResult(),
            playingEpisodes = playedEpisodeTestDataList,
            likedEpisodes = likedEpisodeTestDataList,
            followedPodcasts = followedPodcastTestDataList,
        )
    }
}