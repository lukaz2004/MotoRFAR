package com.vagell.kv4pht.ui.compose.state

data class GroupMember(
    val alias: String,
    val lat: Double,
    val lon: Double,
    val distanceM: Int,
    val bearing: Float,
    val lastSeenMs: Long
) {
    fun isStale(nowMs: Long, thresholdMs: Long = 5 * 60 * 1000L): Boolean =
        (nowMs - lastSeenMs) > thresholdMs
}
