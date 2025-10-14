package io.jacob.episodive.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews

@Composable
fun EpisodiveSearchBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    isExpandable: Boolean = true,
    isExpanded: Boolean = false,
    placeholder: @Composable () -> Unit = { Text("What do you want to listen to?") },
    leadingIconOnCollapse: @Composable () -> Unit = {
        Icon(
            EpisodiveIcons.Search,
            contentDescription = "Search"
        )
    },
    leadingIconOnExpand: @Composable () -> Unit = {
        Icon(
            EpisodiveIcons.ArrowBack,
            contentDescription = "Back",
        )
    },
    trailingIcon: @Composable () -> Unit = {
        Icon(
            imageVector = EpisodiveIcons.Close,
            contentDescription = "Clear",
        )
    },
    contentOnCollapse: @Composable () -> Unit = {},
    contentOnExpand: @Composable (LazyListState) -> Unit = {},
) {
    var expanded by rememberSaveable { mutableStateOf(isExpanded) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberLazyListState()

    val horizontalPadding by animateDpAsState(
        targetValue = if (expanded) 0.dp else 16.dp,
        label = "padding"
    )

    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            keyboardController?.hide()
        }
    }

    Column(
        modifier = modifier,
    ) {
        SearchBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontalPadding)
                .focusRequester(focusRequester),
            shape = RoundedCornerShape(16.dp),
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = {
                        onSearch(query)
                        keyboardController?.hide()
                    },
                    expanded = expanded,
                    onExpandedChange = { if (isExpandable) expanded = it },
                    placeholder = placeholder,
                    leadingIcon = {
                        if (expanded) {
                            IconButton(
                                onClick = { if (isExpandable) expanded = false }
                            ) {
                                leadingIconOnExpand()
                            }
                        } else {
                            leadingIconOnCollapse()
                        }
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    onQueryChange("")
                                    focusRequester.requestFocus()
                                }
                            ) {
                                trailingIcon()
                            }
                        }
                    }
                )
            },
            expanded = expanded,
            onExpandedChange = { if (isExpandable) expanded = it },
        ) {
            contentOnExpand(scrollState)
        }

        if (!expanded) {
            contentOnCollapse()
        }
    }
}

@DevicePreviews
@Composable
private fun EpisodiveSearchBarCollapsePreview() {
    EpisodiveTheme {
        EpisodiveSearchBar(
            query = "search",
            onQueryChange = {},
            onSearch = {},
            isExpanded = false,
            contentOnCollapse = {
                Text(
                    text = "Collapsed content",
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            contentOnExpand = { _ ->
                Text(
                    text = "Expanded content",
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        )
    }
}

@DevicePreviews
@Composable
private fun EpisodiveSearchBarExpandPreview() {
    EpisodiveTheme {
        EpisodiveSearchBar(
            query = "search",
            onQueryChange = {},
            onSearch = {},
            isExpanded = true,
            contentOnCollapse = {
                Text(
                    text = "Collapsed content",
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            contentOnExpand = { _ ->
                Text(
                    text = "Expanded content",
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        )
    }
}