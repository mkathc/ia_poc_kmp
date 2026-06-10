package com.katha.mep.mep_ia_poc.contracts

import com.katha.mep.mep_ia_poc.config.FakeGatewayScenario
import com.katha.mep.mep_ia_poc.models.chat.ChatEvent
import com.katha.mep.mep_ia_poc.models.chat.ChatRequest
import kotlinx.coroutines.flow.Flow

interface ChatProviderAdapter {
    fun sendMessage(
        request: ChatRequest,
        scenario: FakeGatewayScenario,
    ): Flow<ChatEvent>
}
