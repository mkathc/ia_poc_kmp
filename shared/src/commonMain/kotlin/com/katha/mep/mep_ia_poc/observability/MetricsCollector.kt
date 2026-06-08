package com.katha.mep.mep_ia_poc.observability

data class AiMetricEvent(
    val name: String,
    val attributes: Map<String, Any?> = emptyMap(),
)

class MetricsCollector {
    private val events = mutableListOf<AiMetricEvent>()

    fun record(name: String, attributes: Map<String, Any?> = emptyMap()) {
        events += AiMetricEvent(name, attributes)
        println("MetricsCollector metric=$name attributes=$attributes")
    }

    fun export(): List<AiMetricEvent> = events.toList()

    fun clear() {
        events.clear()
    }
}
