package ar.motorfar.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RoutePointDao {
    @Insert
    fun insert(point: RoutePoint)

    @Query("SELECT MAX(sessionId) FROM route_points WHERE alias = :alias")
    fun getLatestSessionId(alias: String): Long?

    @Query("SELECT * FROM route_points WHERE alias = :alias AND sessionId = :sessionId ORDER BY timestamp ASC")
    fun getPointsForSession(alias: String, sessionId: Long): List<RoutePoint>

    @Query("""
        SELECT sessionId, MIN(timestamp) AS startedAt, MAX(timestamp) AS endedAt, COUNT(*) AS pointCount
        FROM route_points WHERE alias = :alias
        GROUP BY sessionId ORDER BY sessionId DESC
    """)
    fun getSessionSummaries(alias: String): List<RouteSessionSummary>

    @Query("DELETE FROM route_points WHERE alias = :alias AND sessionId = :sessionId")
    fun deleteSession(alias: String, sessionId: Long)

    @Query("DELETE FROM route_points WHERE alias = :alias")
    fun deleteForAlias(alias: String)

    @Query("DELETE FROM route_points")
    fun deleteAll()
}
