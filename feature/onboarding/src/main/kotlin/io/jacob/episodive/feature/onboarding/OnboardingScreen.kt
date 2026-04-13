package io.jacob.episodive.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import io.jacob.episodive.core.designsystem.component.EpisodiveButton
import io.jacob.episodive.core.designsystem.component.EpisodiveGradientBackground
import io.jacob.episodive.core.designsystem.component.LoadingWheel
import io.jacob.episodive.core.designsystem.component.scrollbar.DraggableScrollbar
import io.jacob.episodive.core.designsystem.component.scrollbar.scrollbarState
import io.jacob.episodive.core.designsystem.screen.ErrorScreen
import io.jacob.episodive.core.designsystem.screen.LoadingScreen
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.GradientColors
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.SelectableCategory
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.designsystem.component.EpisodiveFilterChip
import io.jacob.episodive.core.ui.PodcastDetailItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun OnboardingRoute(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { OnboardingPage.count })

    val moreCategories = stringResource(R.string.feature_onboarding_category_more_categories)
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is OnboardingEffect.ToastMoreCategories -> onShowSnackbar(moreCategories, null)
                is OnboardingEffect.MoveToPage ->
                    pagerState.animateScrollToPage(effect.page.ordinal)
            }
        }
    }

    when (val s = state) {
        is OnboardingState.Loading -> LoadingScreen()

        is OnboardingState.Success -> OnboardingScreen(
            modifier = modifier,
            pagerState = pagerState,
            categories = s.categories,
            podcasts = viewModel.recommendedPodcasts,
            onChooseCategory = { viewModel.sendAction(OnboardingAction.ChooseCategory(it)) },
            onChoosePodcast = { viewModel.sendAction(OnboardingAction.ChoosePodcast(it)) },
            onNextPage = { viewModel.sendAction(OnboardingAction.NextPage) },
        )

        is OnboardingState.Error -> ErrorScreen(message = s.message)
    }
}

@Composable
internal fun OnboardingScreen(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    categories: List<SelectableCategory>,
    podcasts: Flow<PagingData<Podcast>>,
    onChooseCategory: (Category) -> Unit,
    onChoosePodcast: (Podcast) -> Unit,
    onNextPage: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false,
        ) { page ->
            when (OnboardingPage.fromIndex(page)) {
                OnboardingPage.Welcome ->
                    WelcomeScreen()

                OnboardingPage.CategorySelection ->
                    CategorySelectionScreen(
                        modifier = modifier,
                        categories = categories,
                        onCategoryCheckedChanged = onChooseCategory,
                    )

                OnboardingPage.PodcastSelection ->
                    PodcastSelectionScreen(
                        modifier = modifier,
                        podcasts = podcasts,
                        onToggleFollowedPodcast = onChoosePodcast,
                    )

                OnboardingPage.Completion ->
                    CompletionScreen()

                null -> {}
            }
        }

        if (pagerState.currentPage != OnboardingPage.lastIndex()) {
            EpisodiveGradientBackground(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.BottomCenter),
                gradientColors = GradientColors(
                    bottom = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(top = 100.dp)
                        .align(Alignment.BottomCenter),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PagerIndicator(
                        modifier = Modifier
                            .fillMaxWidth(),
                        pageCount = OnboardingPage.count,
                        currentPage = pagerState.currentPage
                    )

                    EpisodiveButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        onClick = onNextPage,
                        text = { Text(text = stringResource(R.string.feature_onboarding_next)) },
                        enabled = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeScreen(
    modifier: Modifier = Modifier,
) {
    EpisodiveGradientBackground(
        modifier = modifier
            .fillMaxSize(),
        gradientColors = GradientColors(
            top = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(50.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.feature_onboarding_undraw_relax_mode),
                    contentDescription = "Welcome Image",
                )

                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = stringResource(R.string.feature_onboarding_welcome_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySelectionScreen(
    modifier: Modifier = Modifier,
    categories: List<SelectableCategory>,
    onCategoryCheckedChanged: (Category) -> Unit,
) {
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp + systemBarsPadding.calculateTopPadding(),
            bottom = 16.dp + systemBarsPadding.calculateBottomPadding() + 64.dp
        ),
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding:categorySelection"),
    ) {
        item {
            Text(
                text = stringResource(R.string.feature_onboarding_category_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }

        item {
            Text(
                text = stringResource(R.string.feature_onboarding_category_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach {
                    EpisodiveFilterChip(
                        selected = it.isSelected,
                        onSelectedChange = { _ -> onCategoryCheckedChanged(it.category) },
                        label = { Text(text = it.category.label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PodcastSelectionScreen(
    modifier: Modifier = Modifier,
    podcasts: Flow<PagingData<Podcast>>,
    onToggleFollowedPodcast: (Podcast) -> Unit,
) {
    val podcastsPaging = podcasts.collectAsLazyPagingItems()
    val lazyListState = rememberLazyListState()
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        val podcastsSize = podcastsPaging.itemCount

        if (podcastsSize == 0) {
            LoadingWheel(
                modifier = Modifier.align(Alignment.Center),
            )
            return
        }

        LazyColumn(
            state = lazyListState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp + systemBarsPadding.calculateTopPadding(),
                bottom = 16.dp + systemBarsPadding.calculateBottomPadding() + 64.dp
            ),
            modifier = Modifier
                .fillMaxSize()
                .testTag("onboarding:podcastSelection"),
        ) {
            item {
                Text(
                    text = stringResource(R.string.feature_onboarding_podcast_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }

            item {
                Text(
                    text = stringResource(R.string.feature_onboarding_podcast_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            items(
                count = podcastsPaging.itemCount,
                key = { podcastsPaging.peek(it)?.id ?: it },
            ) { index ->
                val podcast = podcastsPaging[index] ?: return@items

                PodcastDetailItem(
                    podcast = podcast,
                    onClick = { onToggleFollowedPodcast(podcast) },
                    onToggleFollowed = { onToggleFollowedPodcast(podcast) },
                )
            }
        }
        lazyListState.DraggableScrollbar(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 12.dp)
                .align(Alignment.TopEnd),
            state = lazyListState.scrollbarState(itemsAvailable = podcastsSize),
            orientation = Orientation.Vertical,
            onThumbMoved = { thumbPosition ->
                scope.launch {
                    val itemIndex = (thumbPosition * podcastsSize).toInt()
                        .coerceIn(0, podcastsSize - 1)
                    lazyListState.scrollToItem(itemIndex)
                }
            }
        )
    }
}

@Composable
private fun CompletionScreen(
    modifier: Modifier = Modifier,
) {
    val thickStrokeWidth = with(LocalDensity.current) { 6.dp.toPx() }
    val thickStroke =
        remember(thickStrokeWidth) { Stroke(width = thickStrokeWidth, cap = StrokeCap.Round) }

    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = modifier
                .padding(horizontal = 50.dp),
            verticalArrangement = Arrangement.spacedBy(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.feature_onboarding_undraw_to_the_moon),
                contentDescription = "Welcome Image",
            )

            LinearWavyProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(),
                stroke = thickStroke,
                trackColor = MaterialTheme.colorScheme.outline,
                trackStroke = thickStroke,
            )

            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = stringResource(R.string.feature_onboarding_completion_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

        }
    }
}

@Composable
private fun PagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { page ->
            Box(
                modifier = Modifier
                    .width(if (page == currentPage) 24.dp else 8.dp)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (page == currentPage)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
            )
            if (page < pageCount - 1) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@DevicePreviews
@Composable
private fun WelcomeScreenPreview() {
    EpisodiveTheme {
        WelcomeScreen()
    }
}

@DevicePreviews
@Composable
private fun CategorySelectionScreenPreview() {
    EpisodiveTheme {
        CategorySelectionScreen(
            categories = Category.entries.map { SelectableCategory(it, false) },
            onCategoryCheckedChanged = {},
        )
    }
}

@DevicePreviews
@Composable
private fun PodcastSelectionScreenPreview() {
    EpisodiveTheme {
        PodcastSelectionScreen(
            podcasts = flowOf(PagingData.from(podcastTestDataList)),
            onToggleFollowedPodcast = {},
        )
    }
}

@DevicePreviews
@Composable
private fun CompletionScreenPreview() {
    EpisodiveTheme {
        CompletionScreen()
    }
}