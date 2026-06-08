package com.katha.mep.mep_ia_poc.network

import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.ContentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class SseEvent(
    val event: String,
    val data: String,
)

class SseHttpException(
    val statusCode: Int,
    message: String,
) : Exception(message)

class SseClient(
    private val httpClientProvider: HttpClientProvider,
) {
    fun streamPost(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): Flow<SseEvent> = flow {
        httpClientProvider.client.preparePost(url) {
            contentType(ContentType.Application.Json)
            headers.forEach { (name, value) -> header(name, value) }
            header(HttpHeaders.Accept, "text/event-stream")
            setBody(body)
        }.execute { response ->
            if (response.status == HttpStatusCode.ServiceUnavailable) {
                throw SseHttpException(response.status.value, "Service unavailable")
            }
            if (!response.status.isSuccess()) {
                throw SseHttpException(response.status.value, "HTTP ${response.status.value}")
            }

            val channel = response.bodyAsChannel()
            var eventName = "message"
            val dataLines = mutableListOf<String>()

            suspend fun emitPending() {
                if (dataLines.isNotEmpty()) {
                    emit(SseEvent(event = eventName, data = dataLines.joinToString("\n")))
                    eventName = "message"
                    dataLines.clear()
                }
            }

            while (true) {
                val line = channel.readUTF8Line() ?: break
                when {
                    line.isEmpty() -> emitPending()
                    line.startsWith(":") -> Unit
                    line.startsWith("event:") -> eventName = line.removePrefix("event:").trim()
                    line.startsWith("data:") -> dataLines += line.removePrefix("data:").trimStart()
                }
            }
            emitPending()
        }
    }
}
