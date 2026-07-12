package ar.motorfar.app.data

data class RouteSessionSummary(
    val sessionId: Long,
    val startedAt: Long,
    val endedAt: Long,
    val pointCount: Int
)
