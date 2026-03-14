package io.jacob.episodive.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.domain.usecase.podcast.GetUserRecommendedPodcastsPagingUseCase
import io.jacob.episodive.core.domain.usecase.podcast.ToggleFollowedUseCase
import io.jacob.episodive.core.domain.usecase.user.GetPreferredCategoriesUseCase
import io.jacob.episodive.core.domain.usecase.user.SetFirstLaunchOffUseCase
import io.jacob.episodive.core.domain.usecase.user.ToggleCategoryUseCase
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.SelectableCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val setFirstLaunchOffUseCase: SetFirstLaunchOffUseCase,
    private val toggleCategoryUseCase: ToggleCategoryUseCase,
    private val toggleFollowedUseCase: ToggleFollowedUseCase,
    private val getPreferredCategoriesUseCase: GetPreferredCategoriesUseCase,
    getUserRecommendedPodcastsPagingUseCase: GetUserRecommendedPodcastsPagingUseCase,
) : ViewModel() {

    private val _page = MutableStateFlow(OnboardingPage.Welcome)
    private val _categories: Flow<List<SelectableCategory>> =
        getPreferredCategoriesUseCase().flatMapLatest { preferredCategories ->
            Timber.d("preferredCategories: $preferredCategories")
            Category.entries.map { category ->
                SelectableCategory(
                    category = category,
                    isSelected = preferredCategories.contains(category),
                )
            }.let { flowOf(it) }
        }
    val recommendedPodcasts: Flow<PagingData<Podcast>> = _page
        .flatMapLatest { page ->
            if (page == OnboardingPage.PodcastSelection) {
                getUserRecommendedPodcastsPagingUseCase(max = 50)
            } else {
                flowOf(PagingData.empty())
            }
        }.cachedIn(viewModelScope)

    val state: StateFlow<OnboardingState> = _categories.map { categories ->
        OnboardingState.Success(
            categories = categories,
        ) as OnboardingState
    }.catch { e ->
        emit(OnboardingState.Error(e.message ?: "An unknown error occurred"))
        e.printStackTrace()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OnboardingState.Loading
    )

    private val _action = MutableSharedFlow<OnboardingAction>(extraBufferCapacity = 1)

    private val _effect = MutableSharedFlow<OnboardingEffect>(extraBufferCapacity = 1)
    val effect = _effect.asSharedFlow()

    init {
        handleActions()
    }

    private fun handleActions() = viewModelScope.launch {
        _action.collectLatest { action ->
            when (action) {
                is OnboardingAction.NextPage -> nextPage()
                is OnboardingAction.PreviousPage -> previousPage()
                is OnboardingAction.ChooseCategory -> chooseCategory(action.category)
                is OnboardingAction.ChoosePodcast -> choosePodcast(action.podcast)
            }
        }
    }

    fun sendAction(action: OnboardingAction) = viewModelScope.launch {
        _action.emit(action)
    }

    private fun nextPage() = viewModelScope.launch {
        when (_page.value) {
            OnboardingPage.CategorySelection -> {
                if (getPreferredCategoriesUseCase().first().size < 3) {
                    _effect.emit(OnboardingEffect.ToastMoreCategories)
                    return@launch
                }
            }

            OnboardingPage.PodcastSelection -> {
                finishOnboarding()
            }

            else -> {}
        }

        _page.value.next()?.let { _page.emit(it) }
        _effect.emit(OnboardingEffect.MoveToPage(_page.value))
    }

    private fun previousPage() = viewModelScope.launch {
        when (_page.value) {
            OnboardingPage.Welcome,
            OnboardingPage.Completion,
                -> return@launch

            else -> {}
        }

        _page.value.previous()?.let { _page.emit(it) }
        _effect.emit(OnboardingEffect.MoveToPage(_page.value))
    }

    private fun chooseCategory(category: Category) = viewModelScope.launch {
        toggleCategoryUseCase(category)
    }

    private fun choosePodcast(podcast: Podcast) = viewModelScope.launch {
        toggleFollowedUseCase(podcast.id)
    }

    private fun finishOnboarding() = viewModelScope.launch {
        delay(3000L)
        setFirstLaunchOffUseCase()
    }
}

sealed interface OnboardingState {
    data object Loading : OnboardingState
    data class Success(
        val categories: List<SelectableCategory>,
    ) : OnboardingState

    data class Error(val message: String) : OnboardingState
}

sealed interface OnboardingAction {
    data object NextPage : OnboardingAction
    data object PreviousPage : OnboardingAction
    data class ChooseCategory(val category: Category) : OnboardingAction
    data class ChoosePodcast(val podcast: Podcast) : OnboardingAction
}

sealed interface OnboardingEffect {
    data object ToastMoreCategories : OnboardingEffect
    data class MoveToPage(val page: OnboardingPage) : OnboardingEffect
}

enum class OnboardingPage {
    Welcome, CategorySelection, PodcastSelection, Completion;

    fun next() = entries.getOrNull(ordinal.plus(1))
    fun previous() = entries.getOrNull(ordinal.minus(1))

    companion object {
        val count = entries.size
        fun fromIndex(index: Int) = entries.getOrNull(index)
        fun firstIndex() = entries.first().ordinal
        fun lastIndex() = entries.last().ordinal
    }
}