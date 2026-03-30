package io.jacob.episodive.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.common.combine
import io.jacob.episodive.core.domain.usecase.FindInLibraryUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetAllPlayedEpisodesPagingUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetAllPlayedEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetLikedEpisodesPagingUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetLikedEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetSavedEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetSavedEpisodesPagingUseCase
import io.jacob.episodive.core.domain.usecase.episode.SaveEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.ResumeEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetFollowedPodcastsPagingUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetFollowedPodcastsUseCase
import io.jacob.episodive.core.domain.usecase.podcast.ToggleFollowedUseCase
import io.jacob.episodive.core.domain.usecase.user.GetPreferredCategoriesUseCase
import io.jacob.episodive.core.domain.usecase.user.GetSelectableCategoriesUseCase
import io.jacob.episodive.core.domain.usecase.user.ToggleCategoryUseCase
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.LibraryFindResult
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.SelectableCategory
import io.jacob.episodive.core.model.mapper.toHumanReadable
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val findInLibraryUseCase: FindInLibraryUseCase,
    getAllPlayedEpisodesUseCase: GetAllPlayedEpisodesUseCase,
    getLikedEpisodesUseCase: GetLikedEpisodesUseCase,
    getSavedEpisodesUseCase: GetSavedEpisodesUseCase,
    getSavedEpisodesPagingUseCase: GetSavedEpisodesPagingUseCase,
    getFollowedPodcastsUseCase: GetFollowedPodcastsUseCase,
    getPreferredCategoriesUseCase: GetPreferredCategoriesUseCase,
    getSelectableCategoriesUseCase: GetSelectableCategoriesUseCase,
    getAllPlayedEpisodesPagingUseCase: GetAllPlayedEpisodesPagingUseCase,
    getLikedEpisodesPagingUseCase: GetLikedEpisodesPagingUseCase,
    getFollowedPodcastsPagingUseCase: GetFollowedPodcastsPagingUseCase,
    private val playEpisodeUseCase: PlayEpisodeUseCase,
    private val resumeEpisodeUseCase: ResumeEpisodeUseCase,
    private val toggleLikedEpisodeUseCase: ToggleLikedEpisodeUseCase,
    private val saveEpisodeUseCase: SaveEpisodeUseCase,
    private val toggleFollowedUseCase: ToggleFollowedUseCase,
    private val toggleCategoryUseCase: ToggleCategoryUseCase,
) : ViewModel() {
    private val _findQuery = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    private val _findResult: Flow<LibraryFindResult> = _findQuery
        .debounce(500L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isNotEmpty()) {
                findInLibraryUseCase(query)
            } else {
                flowOf(LibraryFindResult())
            }
        }

    private val _section = MutableStateFlow(LibrarySection.All)

    val playedEpisodesPaging: Flow<PagingData<SeparatedUiModel<Episode>>> =
        getAllPlayedEpisodesPagingUseCase().map { pagingData ->
            pagingData
                .map { episode -> SeparatedUiModel.Content(episode) }
                .insertSeparators { before, after ->
                    if (after == null) {
                        return@insertSeparators null
                    }

                    val beforeDate = before?.data?.playedAt?.toHumanReadable()
                    val afterDate = after.data.playedAt?.toHumanReadable()

                    if (before == null || beforeDate != afterDate) {
                        afterDate?.let { SeparatedUiModel.Separator(it) }
                    } else {
                        null
                    }
                }
        }.cachedIn(viewModelScope)

    val likedEpisodesPaging: Flow<PagingData<SeparatedUiModel<Episode>>> =
        getLikedEpisodesPagingUseCase().map { pagingData ->
            pagingData
                .map { episode -> SeparatedUiModel.Content(episode) }
                .insertSeparators { before, after ->
                    if (after == null) {
                        return@insertSeparators null
                    }

                    val beforeDate = before?.data?.likedAt?.toHumanReadable()
                    val afterDate = after.data.likedAt?.toHumanReadable()

                    if (before == null || beforeDate != afterDate) {
                        afterDate?.let { SeparatedUiModel.Separator(it) }
                    } else {
                        null
                    }
                }
        }.cachedIn(viewModelScope)

    val savedEpisodesPaging: Flow<PagingData<SeparatedUiModel<Episode>>> =
        getSavedEpisodesPagingUseCase().map { pagingData ->
            pagingData
                .map { episode -> SeparatedUiModel.Content(episode) }
                .insertSeparators { before, after ->
                    if (after == null) {
                        return@insertSeparators null
                    }

                    val beforeDate = before?.data?.savedAt?.toHumanReadable()
                    val afterDate = after.data.savedAt?.toHumanReadable()

                    if (before == null || beforeDate != afterDate) {
                        afterDate?.let { SeparatedUiModel.Separator(it) }
                    } else {
                        null
                    }
                }
        }.cachedIn(viewModelScope)

    val followedPodcastsPaging: Flow<PagingData<SeparatedUiModel<Podcast>>> =
        getFollowedPodcastsPagingUseCase().map { pagingData ->
            pagingData
                .map { podcast -> SeparatedUiModel.Content(podcast) }
                .insertSeparators { before, after ->
                    if (after == null) {
                        return@insertSeparators null
                    }

                    val beforeDate = before?.data?.followedAt?.toHumanReadable()
                    val afterDate = after.data.followedAt?.toHumanReadable()

                    if (before == null || beforeDate != afterDate) {
                        afterDate?.let { SeparatedUiModel.Separator(it) }
                    } else {
                        null
                    }
                }
        }.cachedIn(viewModelScope)

    val state: StateFlow<LibraryState> = combine(
        _findQuery,
        _findResult,
        getAllPlayedEpisodesUseCase(max = SECTION_MAX),
        getLikedEpisodesUseCase(max = SECTION_MAX),
        getSavedEpisodesUseCase(max = SECTION_MAX),
        getFollowedPodcastsUseCase(max = SECTION_MAX),
        getPreferredCategoriesUseCase(),
        getSelectableCategoriesUseCase(),
        _section
    ) { query, result, allPlayedEpisodes, likedEpisodes, savedEpisodes, followedPodcasts, preferredCategories, selectableCategories, section ->
        if (query.isEmpty() && result.isAllEmpty) {
            LibraryState.Success(
                findQuery = query,
                allPlayedEpisodes = allPlayedEpisodes,
                likedEpisodes = likedEpisodes,
                savedEpisodes = savedEpisodes,
                followedPodcasts = followedPodcasts,
                preferredCategories = preferredCategories,
                selectableCategories = selectableCategories,
                section = section,
            ) as LibraryState
        } else {
            LibraryState.Success(
                findQuery = query,
                allPlayedEpisodes = result.playingEpisodes,
                likedEpisodes = result.likedEpisodes,
                savedEpisodes = result.savedEpisodes,
                followedPodcasts = result.followedPodcasts,
                preferredCategories = emptyList(),
                selectableCategories = selectableCategories,
                section = section,
            ) as LibraryState
        }
    }.catch { e ->
        emit(LibraryState.Error(e.message ?: "An unknown error occurred"))
        e.printStackTrace()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryState.Loading,
    )

    private val _action = MutableSharedFlow<LibraryAction>(extraBufferCapacity = 1)

    private val _effect = MutableSharedFlow<LibraryEffect>(extraBufferCapacity = 1)
    val effect = _effect.asSharedFlow()

    init {
        handleActions()
    }

    private fun handleActions() = viewModelScope.launch {
        _action.collectLatest { action ->
            when (action) {
                is LibraryAction.QueryChanged -> changeQuery(action.query)
                is LibraryAction.ClickFind -> changeQuery(action.query)
                is LibraryAction.ClearQuery -> clearQuery()
                is LibraryAction.ClickPlayingEpisode -> resumeEpisode(action.episode)
                is LibraryAction.ClickEpisode -> playEpisode(action.episode)
                is LibraryAction.ClickPodcast -> clickPodcast(action.podcast)
                is LibraryAction.ToggleLikedEpisode -> toggleLikedEpisode(action.episode)
                is LibraryAction.ToggleSavedEpisode -> toggleSavedEpisode(action.episode)
                is LibraryAction.ToggleFollowedPodcast -> toggleFollowedPodcast(action.podcast)
                is LibraryAction.TogglePreferredCategory -> toggleCategory(action.category)
                is LibraryAction.SelectSection -> selectSection(action.section)
            }
        }
    }

    fun sendAction(action: LibraryAction) = viewModelScope.launch {
        _action.emit(action)
    }

    private fun changeQuery(query: String) = viewModelScope.launch {
        _findQuery.emit(query)
    }

    private fun clearQuery() = viewModelScope.launch {
        _findQuery.emit("")
    }

    private fun resumeEpisode(playedEpisode: Episode) = viewModelScope.launch {
        if (playedEpisode.isCompleted) {
            playEpisodeUseCase(playedEpisode)
        } else {
            resumeEpisodeUseCase(playedEpisode)
        }
    }

    private fun playEpisode(episode: Episode) = viewModelScope.launch {
        playEpisodeUseCase(episode)
    }

    private fun clickPodcast(podcast: Podcast) = viewModelScope.launch {
        _effect.emit(LibraryEffect.NavigateToPodcast(podcast.id))
    }

    private fun toggleLikedEpisode(episode: Episode) = viewModelScope.launch {
        toggleLikedEpisodeUseCase(episode)
    }

    private fun toggleSavedEpisode(episode: Episode) = viewModelScope.launch {
        saveEpisodeUseCase(episode)
    }

    private fun toggleFollowedPodcast(followedPodcast: Podcast) = viewModelScope.launch {
        toggleFollowedUseCase(followedPodcast.id)
    }

    private fun toggleCategory(category: Category) = viewModelScope.launch {
        toggleCategoryUseCase(category)
    }

    private fun selectSection(section: LibrarySection) = viewModelScope.launch {
        _section.emit(section)
    }

    companion object {
        private const val SECTION_MAX = 10
    }
}

sealed interface LibraryState {
    data object Loading : LibraryState
    data class Success(
        val findQuery: String,
        val allPlayedEpisodes: List<Episode>,
        val likedEpisodes: List<Episode>,
        val savedEpisodes: List<Episode>,
        val followedPodcasts: List<Podcast>,
        val preferredCategories: List<Category>,
        val selectableCategories: List<SelectableCategory>,
        val section: LibrarySection,
    ) : LibraryState

    data class Error(val message: String) : LibraryState
}

sealed interface LibraryAction {
    data class QueryChanged(val query: String) : LibraryAction
    data class ClickFind(val query: String) : LibraryAction
    data object ClearQuery : LibraryAction
    data class ClickPlayingEpisode(val episode: Episode) : LibraryAction
    data class ClickEpisode(val episode: Episode) : LibraryAction
    data class ClickPodcast(val podcast: Podcast) : LibraryAction
    data class ToggleLikedEpisode(val episode: Episode) : LibraryAction
    data class ToggleSavedEpisode(val episode: Episode) : LibraryAction
    data class ToggleFollowedPodcast(val podcast: Podcast) : LibraryAction
    data class TogglePreferredCategory(val category: Category) : LibraryAction
    data class SelectSection(val section: LibrarySection) : LibraryAction
}

sealed interface LibraryEffect {
    data class NavigateToPodcast(val podcastId: Long) : LibraryEffect
}

enum class LibrarySection { All, RecentlyListened, Liked, Saved, Followed, Preferred; }

sealed interface SeparatedUiModel<out T> {
    data class Separator(val label: String) : SeparatedUiModel<Nothing>
    data class Content<T>(val data: T) : SeparatedUiModel<T>
}