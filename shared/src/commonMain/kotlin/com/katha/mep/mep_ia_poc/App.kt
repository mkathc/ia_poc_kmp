package com.katha.mep.mep_ia_poc

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.katha.mep.mep_ia_poc.chat.ChatScreen
import com.katha.mep.mep_ia_poc.chat.ChatViewModel
import com.katha.mep.mep_ia_poc.ui.components.PlaceholderPanel
import com.katha.mep.mep_ia_poc.ui.navigation.AppTab
import com.katha.mep.mep_ia_poc.viewmodel.AppViewModel
import com.katha.mep.mep_ia_poc.viewmodel.previewAppViewModel
import com.katha.mep.mep_ia_poc.viewmodel.previewChatViewModel

@Composable
fun App(
    viewModel: AppViewModel,
    chatViewModel: ChatViewModel,
) {
    MaterialTheme {
        var selectedTab by remember { mutableStateOf(AppTab.Chat) }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    AppTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text(tab.label) },
                            icon = { Text(tab.label.take(1)) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                when (selectedTab) {
                    AppTab.Chat -> ChatScreen(
                        viewModel = chatViewModel,
                        appViewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                    )
                    AppTab.Home -> PlaceholderScreen(
                        title = "Home",
                        description = "Home personalization pending",
                    )
                    AppTab.Search -> PlaceholderScreen(
                        title = "Search",
                        description = "Intelligent search pending",
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    description: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        PlaceholderPanel(
            title = title,
            description = description,
        )
    }
}

@Composable
@Preview
fun AppPreview() {
    val appViewModel = previewAppViewModel()
    App(
        viewModel = appViewModel,
        chatViewModel = previewChatViewModel(appViewModel),
    )
}
