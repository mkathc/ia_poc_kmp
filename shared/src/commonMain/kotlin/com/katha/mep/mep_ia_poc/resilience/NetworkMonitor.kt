package com.katha.mep.mep_ia_poc.resilience

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NetworkMonitor {
    private val mutableIsOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = mutableIsOnline

    fun updateOnlineState(isOnline: Boolean) {
        mutableIsOnline.value = isOnline
    }
}
