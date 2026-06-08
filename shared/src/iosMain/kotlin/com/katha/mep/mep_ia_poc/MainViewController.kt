package com.katha.mep.mep_ia_poc

import androidx.compose.ui.window.ComposeUIViewController
import com.katha.mep.mep_ia_poc.di.AppDi

fun MainViewController() = ComposeUIViewController {
    App(
        viewModel = AppDi.appViewModel,
        chatViewModel = AppDi.chatViewModel,
    )
}
