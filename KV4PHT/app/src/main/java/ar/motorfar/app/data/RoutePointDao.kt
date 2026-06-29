package ar.motorfar.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RoutePointDao {
    @Insert
    fun insert(point: RoutePoint)

    @Query("SELECT * FROM route_points WHERE alias = :alias ORDER BY timestamp ASC")
    fun getPointsForAlias(alias: String): List<RoutePoint>

    @Query("DELETE FROM route_points")
    fun deleteAll()
}
