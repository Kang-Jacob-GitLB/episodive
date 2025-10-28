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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
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
import io.jacob.episodive.core.designsystem.component.CategoryButton
import io.jacob.episodive.core.designsystem.component.EpisodiveButton
import io.jacob.episodive.core.designsystem.component.EpisodiveGradientBackground
import io.jacob.episodive.core.designsystem.component.PodcastDetailItem
import io.jacob.episodive.core.designsystem.component.scrollbar.DraggableScrollbar
import io.jacob.episodive.core.designsystem.component.scrollbar.scrollbarState
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.GradientColors
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.FollowablePodcast
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.SelectableCategory
import io.jacob.episodive.core.testing.model.podcastTestDataList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
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
                        categories = state.categories,
                        onCategoryCheckedChanged = { category ->
                            viewModel.sendAction(OnboardingAction.ChooseCategory(category))
                        },
                    )

                OnboardingPage.PodcastSelection ->
                    PodcastSelectionScreen(
                        modifier = modifier,
                        followablePodcasts = state.podcasts,
                        onPodcastCheckedChanged = { podcast ->
                            viewModel.sendAction(OnboardingAction.ChoosePodcast(podcast))
                        },
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
                        onClick = { viewModel.sendAction(OnboardingAction.NextPage) },
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

@Composable
private fun CategorySelectionScreen(
    modifier: Modifier = Modifier,
    categories: List<SelectableCategory>,
    onCategoryCheckedChanged: (Category) -> Unit,
) {
    val lazyGridState = rememberLazyGridState()
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp + systemBarsPadding.calculateTopPadding(),
                bottom = 16.dp + systemBarsPadding.calculateBottomPadding() + 64.dp
            ),
            modifier = Modifier
                .fillMaxSize()
                .testTag("onboarding:categorySelection"),
        ) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = stringResource(R.string.feature_onboarding_category_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }

            item(span = { GridItemSpan(2) }) {
                Text(
                    text = stringResource(R.string.feature_onboarding_category_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            items(
                items = categories,
                key = { it.category.id },
            ) {
                CategoryButton(
                    modifier = Modifier
                        .aspectRatio(1f),
                    category = it.category,
                    isSelected = it.isSelected,
                    onClick = onCategoryCheckedChanged
                )
            }
        }
        lazyGridState.DraggableScrollbar(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 12.dp)
                .align(Alignment.TopEnd),
            state = lazyGridState.scrollbarState(itemsAvailable = categories.size),
            orientation = Orientation.Vertical,
            onThumbMoved = { thumbPosition ->
                scope.launch {
                    val itemIndex = (thumbPosition * categories.size).toInt()
                    lazyGridState.scrollToItem(itemIndex)
                }
            }
        )
    }
}

@Composable
private fun PodcastSelectionScreen(
    modifier: Modifier = Modifier,
    followablePodcasts: List<FollowablePodcast>,
    onPodcastCheckedChanged: (Podcast) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
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
                count = followablePodcasts.size,
                key = { followablePodcasts[it].podcast.id },
            ) {
                PodcastDetailItem(
                    podcast = followablePodcasts[it].podcast,
                    isFollowed = followablePodcasts[it].isFollow,
                    onClick = { onPodcastCheckedChanged(followablePodcasts[it].podcast) },
                    onToggleFollowed = { onPodcastCheckedChanged(followablePodcasts[it].podcast) },
                )
            }
        }
        lazyListState.DraggableScrollbar(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 12.dp)
                .align(Alignment.TopEnd),
            state = lazyListState.scrollbarState(itemsAvailable = followablePodcasts.size),
            orientation = Orientation.Vertical,
            onThumbMoved = { thumbPosition ->
                scope.launch {
                    val itemIndex = (thumbPosition * followablePodcasts.size).toInt()
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
    modifier: Modifier = Modifier
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
            followablePodcasts = podcastTestDataList.map { FollowablePodcast(it, true) },
            onPodcastCheckedChanged = {},
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