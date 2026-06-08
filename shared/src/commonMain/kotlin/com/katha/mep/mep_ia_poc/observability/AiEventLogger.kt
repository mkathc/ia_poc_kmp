package com.katha.mep.mep_ia_poc.observability

class AiEventLogger {
    fun log(eventName: String, attributes: Map<String, Any?> = emptyMap()) {
        println("AiEventLogger event=$eventName attributes=$attributes")
    }
}
