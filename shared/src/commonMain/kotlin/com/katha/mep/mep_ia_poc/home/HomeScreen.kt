package com.katha.mep.mep_ia_poc.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.katha.mep.mep_ia_poc.config.FakeGatewayScenario
import com.katha.mep.mep_ia_poc.config.MockUserProfile
import com.katha.mep.mep_ia_poc.config.ProviderMock
import com.katha.mep.mep_ia_poc.models.home.ActionSuggestion
import com.katha.mep.mep_ia_poc.models.home.HomeAlert
import com.katha.mep.mep_ia_poc.models.home.HomeCard

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            HomeHeader(
                state = state,
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
            if (state.isLoading || state.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                if (state.isLoading && state.experience == null) {
                    item {
                        HomeLoadingState()
                    }
                }

                state.degradedMessage?.let { message ->
                    item {
                        HomeBanner(message = message)
                    }
                }

                state.errorMessage?.let { message ->
                    item {
                        HomeErrorState(message = message, onRetry = viewModel::refresh)
                    }
                }

                state.experience?.let { experience ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Hola", style = MaterialTheme.typography.headlineSmall)
                            Text("Segmento: ${experience.userSegment}", style = MaterialTheme.typography.bodyMedium)
                            if (experience.isFromCache) {
                                Text("Contenido cacheado", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    items(experience.alerts, key = { it.id }) { alert ->
                        HomeAlertCard(alert)
                    }

                    items(experience.cards, key = { it.id }) { card ->
                        HomeContentCard(card = card, onActionTapped = viewModel::onActionTapped)
                    }

                    if (experience.nextBestActions.isNotEmpty()) {
                        item {
                            Text("Siguientes acciones", style = MaterialTheme.typography.titleMedium)
                        }
                        items(experience.nextBestActions, key = { it.id }) { action ->
                            ActionSuggestionRow(action = action, onActionTapped = viewModel::onActionTapped)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    state: HomeState,
    onScenarioSelected: (FakeGatewayScenario) -> Unit,
    onProviderSelected: (ProviderMock) -> Unit,
    onProfileSelected: (MockUserProfile) -> Unit,
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
            Text("Home", style = MaterialTheme.typography.titleLarge)
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
                entries = MockUserProfile.entries,
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
private fun HomeLoadingState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = "Personalizando Home...",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeBanner(message: String) {
    Surface(
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
private fun HomeErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(message, modifier = Modifier.weight(1f))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun HomeAlertCard(alert: HomeAlert) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(alert.title, style = MaterialTheme.typography.titleSmall)
            Text(alert.message, style = MaterialTheme.typography.bodyMedium)
            Text(alert.severity, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun HomeContentCard(
    card: HomeCard,
    onActionTapped: (ActionSuggestion) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(card.title, style = MaterialTheme.typography.titleMedium)
            Text(card.description, style = MaterialTheme.typography.bodyMedium)
            Text("type=${card.type} priority=${card.priority}", style = MaterialTheme.typography.labelSmall)
            card.action?.let { action ->
                Button(onClick = { onActionTapped(action) }) {
                    Text(action.label)
                }
            }
        }
    }
}

@Composable
private fun ActionSuggestionRow(
    action: ActionSuggestion,
    onActionTapped: (ActionSuggestion) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(action.label, style = MaterialTheme.typography.bodyLarge)
                Text(action.route, style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(onClick = { onActionTapped(action) }) {
                Text("Abrir")
            }
        }
    }
}
