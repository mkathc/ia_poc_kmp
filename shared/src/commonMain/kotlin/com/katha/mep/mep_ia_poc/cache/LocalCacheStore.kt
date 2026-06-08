package com.katha.mep.mep_ia_poc.cache

class LocalCacheStore {
    private val values = mutableMapOf<String, Any?>()

    fun save(key: String, value: Any?) {
        values[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? = values[key] as? T

    fun remove(key: String) {
        values.remove(key)
    }

    fun clear() {
        values.clear()
    }

    companion object {
        const val EmergencyReportsKey = "emergency_reports"

        fun homeKey(providerId: String, profileName: String): String =
            "home_${providerId}_$profileName"

        fun searchKey(providerId: String, profileName: String, normalizedQuery: String): String =
            "search_${providerId}_${profileName}_$normalizedQuery"
    }
}
