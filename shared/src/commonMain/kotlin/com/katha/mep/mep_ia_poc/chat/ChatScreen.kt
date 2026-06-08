package com.katha.mep.mep_ia_poc.chat

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.katha.mep.mep_ia_poc.models.chat.ChatMessage
import com.katha.mep.mep_ia_poc.models.resilience.ExperienceFallback
import com.katha.mep.mep_ia_poc.models.resilience.ExperienceFallbackType
import com.katha.mep.mep_ia_poc.viewmodel.AppViewModel

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val debugState by appViewModel.state.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.text, state.isStreaming) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ChatTopBar(
                scenario = state.activeScenario,
                provider = state.activeProvider,
                useRemoteGateway = debugState.useRemoteGateway,
                onScenarioSelected = viewModel::changeScenario,
                onProviderSelected = viewModel::changeProvider,
                onRemoteToggle = appViewModel::toggleRemoteGateway,
            )
        },
        bottomBar = {
            ChatInputBar(
                value = input,
                onValueChange = { input = it },
                onSend = {
                    viewModel.sendMessage(input)
                    input = ""
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            state.activeFallback?.let { fallback ->
                ChatFallbackBanner(
                    fallback = fallback,
                    onDismiss = viewModel::dismissFallback,
                    onRetry = viewModel::retryLastMessage,
                    onWhatsappFallback = viewModel::openWhatsappFallback,
                )
            }

            if (state.activeFallback == null) {
                state.errorMessage?.let { error ->
                    ChatErrorBanner(
                        error = error,
                        onRetry = viewModel::retryLastMessage,
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    ChatMessageBubble(message)
                }
            }
        }
    }
}

@Composable
private fun ChatTopBar(
    scenario: FakeGatewayScenario,
    provider: ProviderMock,
    useRemoteGateway: Boolean,
    onScenarioSelected: (FakeGatewayScenario) -> Unit,
    onProviderSelected: (ProviderMock) -> Unit,
    onRemoteToggle: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Chat", style = MaterialTheme.typography.titleLarge)
            ChatDebugControls(
                scenario = scenario,
                provider = provider,
                useRemoteGateway = useRemoteGateway,
                onScenarioSelected = onScenarioSelected,
                onProviderSelected = onProviderSelected,
                onRemoteToggle = onRemoteToggle,
            )
        }
    }
}

@Composable
private fun ChatDebugControls(
    scenario: FakeGatewayScenario,
    provider: ProviderMock,
    useRemoteGateway: Boolean,
    onScenarioSelected: (FakeGatewayScenario) -> Unit,
    onProviderSelected: (ProviderMock) -> Unit,
    onRemoteToggle: (Boolean) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DebugDropdown(
                label = provider.id,
                entries = ProviderMock.entries,
                itemLabel = { it.id },
                onSelected = onProviderSelected,
                modifier = Modifier.weight(1f),
            )
            DebugDropdown(
                label = scenario.wireName,
                entries = FakeGatewayScenario.entries,
                itemLabel = { it.wireName },
                onSelected = onScenarioSelected,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Mock gateway",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Switch(
                checked = useRemoteGateway,
                onCheckedChange = onRemoteToggle,
            )
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
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
private fun ChatFallbackBanner(
    fallback: ExperienceFallback,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onWhatsappFallback: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Fallback activo: ${fallback.type}", style = MaterialTheme.typography.titleSmall)
            Text(fallback.userFacingMessage(), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
                OutlinedButton(onClick = onRetry) {
                    Text("Retry")
                }
                if (fallback.type != ExperienceFallbackType.SlaTimeout) {
                    OutlinedButton(onClick = onWhatsappFallback) {
                        Text("WhatsApp")
                    }
                }
            }
        }
    }
}

private fun ExperienceFallback.userFacingMessage(): String =
    when (type) {
        ExperienceFallbackType.Offline -> "No tengo conexión con el asistente cloud por el momento."
        ExperienceFallbackType.ConnectionLost -> "Se perdió la conexión con el asistente. Puedes reintentar."
        ExperienceFallbackType.SlaTimeout -> "El asistente está tardando más de lo esperado. Puedes continuar esperando o reintentar."
        else -> reason
    }

@Composable
private fun ChatErrorBanner(
    error: String,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(error, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            color = if (message.isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp,
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (message.isUser) "Tú" else "Asistente",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = message.text.ifBlank { "..." },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe un mensaje") },
                singleLine = false,
                minLines = 1,
                maxLines = 4,
            )
            Button(
                onClick = onSend,
                enabled = value.isNotBlank(),
                shape = CircleShape,
            ) {
                Text("Enviar")
            }
        }
    }
}
