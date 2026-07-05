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

    @Query("DELETE FROM route_points WHERE alias = :alias")
    fun deleteForAlias(alias: String)

    @Query("DELETE FROM route_points")
    fun deleteAll()
}
