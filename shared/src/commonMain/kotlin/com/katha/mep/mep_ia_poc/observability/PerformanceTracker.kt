package com.katha.mep.mep_ia_poc.observability

class PerformanceTracker(
    private val logger: AiEventLogger,
) {
    fun requestStart(feature: String) = logger.log("requestStart", mapOf("feature" to feature))
    fun firstTokenReceived(messageId: String) = logger.log("firstTokenReceived", mapOf("messageId" to messageId))
    fun responseCompleted(feature: String) = logger.log("responseCompleted", mapOf("feature" to feature))
    fun fallbackTriggered(type: String, reason: String) = logger.log("fallbackTriggered", mapOf("type" to type, "reason" to reason))
    fun homeRefreshTime(elapsedMillis: Long) = logger.log("homeRefreshTime", mapOf("elapsedMillis" to elapsedMillis))
    fun searchResponseTime(elapsedMillis: Long) = logger.log("searchResponseTime", mapOf("elapsedMillis" to elapsedMillis))
    fun emergencyGuideLoadStart() = logger.log("emergencyGuideLoadStart")
    fun emergencyGuideLoadCompleted(elapsedMillis: Long) = logger.log("emergencyGuideLoadCompleted", mapOf("elapsedMillis" to elapsedMillis))
    fun emergencySyncStart() = logger.log("emergencySyncStart")
    fun emergencySyncCompleted() = logger.log("emergencySyncCompleted")
    fun emergencyGuideLoadTime(elapsedMillis: Long) = logger.log("emergencyGuideLoadTime", mapOf("elapsedMillis" to elapsedMillis))
}
