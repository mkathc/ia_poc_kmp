package com.katha.mep.mep_ia_poc

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.katha.mep.mep_ia_poc.home.HomeScreen
import com.katha.mep.mep_ia_poc.home.HomeViewModel
import com.katha.mep.mep_ia_poc.search.SearchScreen
import com.katha.mep.mep_ia_poc.search.SearchViewModel
import com.katha.mep.mep_ia_poc.ui.navigation.AppTab
import com.katha.mep.mep_ia_poc.viewmodel.AppViewModel
import com.katha.mep.mep_ia_poc.viewmodel.previewAppViewModel
import com.katha.mep.mep_ia_poc.viewmodel.previewChatViewModel
import com.katha.mep.mep_ia_poc.viewmodel.previewHomeViewModel
import com.katha.mep.mep_ia_poc.viewmodel.previewSearchViewModel

@Composable
fun App(
    viewModel: AppViewModel,
    homeViewModel: HomeViewModel,
    chatViewModel: ChatViewModel,
    searchViewModel: SearchViewModel,
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
                    AppTab.Home -> HomeScreen(
                        viewModel = homeViewModel,
                        modifier = Modifier.fillMaxSize(),
                    )
                    AppTab.Search -> SearchScreen(
                        viewModel = searchViewModel,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
@Preview
fun AppPreview() {
    val appViewModel = previewAppViewModel()
    App(
        viewModel = appViewModel,
        homeViewModel = previewHomeViewModel(appViewModel),
        chatViewModel = previewChatViewModel(appViewModel),
        searchViewModel = previewSearchViewModel(appViewModel),
    )
}
