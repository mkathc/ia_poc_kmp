package com.katha.mep.mep_ia_poc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.katha.mep.mep_ia_poc.di.AppDi

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(
                viewModel = AppDi.appViewModel,
                homeViewModel = AppDi.homeViewModel,
                chatViewModel = AppDi.chatViewModel,
                searchViewModel = AppDi.searchViewModel,
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(
        viewModel = AppDi.appViewModel,
        homeViewModel = AppDi.homeViewModel,
        chatViewModel = AppDi.chatViewModel,
        searchViewModel = AppDi.searchViewModel,
    )
}
