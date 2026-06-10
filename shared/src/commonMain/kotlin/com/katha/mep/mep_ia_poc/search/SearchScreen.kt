package com.katha.mep.mep_ia_poc.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.katha.mep.mep_ia_poc.config.FakeGatewayScenario
import com.katha.mep.mep_ia_poc.config.ProviderMock
import com.katha.mep.mep_ia_poc.config.SearchProfile
import com.katha.mep.mep_ia_poc.models.search.SearchItem
import com.katha.mep.mep_ia_poc.models.search.SearchSuggestedAction

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            SearchHeader(
                state = state,
                onQueryChanged = viewModel::updateQuery,
                onSearch = viewModel::search,
                onClear = viewModel::clear,
                onScenarioSelected = viewModel::changeScenario,
                onProviderSelected = viewModel::changeProvider,
                onProfileSelected = viewModel::changeProfile,
                onRemoteToggle = viewModel::toggleRemoteGateway,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.isLoading || state.isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                state.degradedMessage?.let { message ->
                    item { SearchBanner(message) }
                }

                state.errorMessage?.let { message ->
                    item {
                        SearchErrorState(
                            message = message,
                            onRetry = viewModel::retry,
                        )
                    }
                }

                if (state.hasSearched && state.errorMessage == null) {
                    item {
                        SearchIntentPanel(
                            detectedIntent = state.detectedIntent,
                            isFromCache = state.isFromCache,
                        )
                    }
                }

                if (state.suggestedActions.isNotEmpty()) {
                    item {
                        SuggestedActionsSection(
                            actions = state.suggestedActions,
                            onActionTapped = viewModel::onActionTapped,
                        )
                    }
                }

                if (state.hasSearched && !state.isSearching && state.errorMessage == null && state.results.isEmpty()) {
                    item { EmptySearchState() }
                }

                items(state.results, key = { it.id }) { result ->
                    SearchResultCard(result = result, onResultTapped = viewModel::onResultTapped)
                }
            }
        }
    }
}

@Composable
private fun SearchHeader(
    state: SearchState,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onScenarioSelected: (FakeGatewayScenario) -> Unit,
    onProviderSelected: (ProviderMock) -> Unit,
    onProfileSelected: (SearchProfile) -> Unit,
    onRemoteToggle: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Search", style = MaterialTheme.typography.titleLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChanged,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Buscar") },
                )
                Button(onClick = onSearch, enabled = state.query.isNotBlank() && !state.isSearching) {
                    Text("Search")
                }
                TextButton(onClick = onClear) {
                    Text("Clear")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DebugDropdown(
                    label = state.activeProvider.id,
                    entries = ProviderMock.entries,
                    itemLabel = { it.id },
                    onSelected = onProviderSelected,
                    modifier = Modifier.weight(1f),
                )
                DebugDropdown(
                    label = state.activeScenario.wireName,
                    entries = FakeGatewayScenario.entries,
                    itemLabel = { it.wireName },
                    onSelected = onScenarioSelected,
                    modifier = Modifier.weight(1f),
                )
            }
            DebugDropdown(
                label = state.activeProfile.profileName,
                entries = SearchProfile.entries,
                itemLabel = { it.profileName },
                onSelected = onProfileSelected,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Mock Gateway",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Switch(
                    checked = state.useRemoteGateway,
                    onCheckedChange = onRemoteToggle,
                )
            }
        }
    }
}

@Composable
private fun <T> DebugDropdown(
    label: String,
    entries: List<T>,
    itemLabel: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(itemLabel(entry)) },
                    onClick = {
                        expanded = false
                        onSelected(entry)
                    },
                )
            }
        }
    }
}

@Composable
private fun SearchBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SearchErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, modifier = Modifier.weight(1f))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun SearchIntentPanel(
    detectedIntent: String,
    isFromCache: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Intent detectado: $detectedIntent", style = MaterialTheme.typography.titleSmall)
            if (isFromCache) {
                Text("Resultados cacheados", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SuggestedActionsSection(
    actions: List<SearchSuggestedAction>,
    onActionTapped: (SearchSuggestedAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Suggested Actions", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            actions.forEach { action ->
                AssistChip(
                    onClick = { onActionTapped(action) },
                    label = { Text(action.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
    }
}

@Composable
private fun EmptySearchState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = "No encontramos resultados para esta búsqueda.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SearchResultCard(
    result: SearchItem,
    onResultTapped: (SearchItem) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(result.title, style = MaterialTheme.typography.titleMedium)
            Text(result.description, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("score=${result.score}", style = MaterialTheme.typography.labelSmall)
            }
            Button(
                onClick = { onResultTapped(result) },
                modifier = Modifier.widthIn(min = 96.dp),
            ) {
                Text("Open")
            }
        }
    }
}
