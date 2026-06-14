package ar.motorfar.app.ui.compose.state

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Destino de navegación: un punto en el mapa hacia el que el grupo se dirige.
 *
 * Puede ser local (marcado por uno mismo) o recibido del líder vía radio.
 * Calcula distancia y rumbo desde una posición dada.
 */
data class Destination(
    val lat: Double,
    val lon: Double,
    val label: String = "DESTINO",
    val fromAlias: String? = null,   // quién lo compartió (null = local)
    val sharedAtMs: Long = System.currentTimeMillis()
) {
    /** Distancia en metros desde (fromLat, fromLon) hasta este destino (Haversine). */
    fun distanceMetersFrom(fromLat: Double, fromLon: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat - fromLat)
        val dLon = Math.toRadians(lon - fromLon)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(fromLat)) * cos(Math.toRadians(lat)) *
                sin(dLon / 2) * sin(dLon / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Rumbo inicial en grados (0-360, 0=Norte) desde (fromLat, fromLon). */
    fun bearingFrom(fromLat: Double, fromLon: Double): Double {
        val dLon = Math.toRadians(lon - fromLon)
        val y = sin(dLon) * cos(Math.toRadians(lat))
        val x = cos(Math.toRadians(fromLat)) * sin(Math.toRadians(lat)) -
                sin(Math.toRadians(fromLat)) * cos(Math.toRadians(lat)) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    /** Serializa a payload APRS compacto: "DEST lat lon LABEL" (<67 chars). */
    fun toAprsPayload(): String =
        "DEST %.5f %.5f %s".format(lat, lon, label.take(12).uppercase())

    companion object {
        /** Punto cardinal abreviado (N, NE, E…) para un rumbo dado. */
        fun cardinal(bearingDeg: Double): String {
            val dirs = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
            val idx = (((bearingDeg + 22.5) % 360) / 45).toInt()
            return dirs[idx % 8]
        }

        /** Formatea una distancia en metros como "850 m" o "12.4 km". */
        fun formatDistance(meters: Double): String =
            if (meters < 1000) "${meters.toInt()} m"
            else "%.1f km".format(meters / 1000.0)

        /**
         * Parsea un payload APRS de destino. Devuelve null si no es un DEST válido.
         * Formato esperado: "DEST -34.54710 -58.52900 PARADOR"
         */
        fun fromAprsPayload(payload: String, fromAlias: String?): Destination? {
            val parts = payload.trim().split(Regex("\\s+"))
            if (parts.size < 3 || parts[0] != "DEST") return null
            val lat = parts[1].toDoubleOrNull() ?: return null
            val lon = parts[2].toDoubleOrNull() ?: return null
            if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
            val label = if (parts.size >= 4) parts.subList(3, parts.size).joinToString(" ") else "DESTINO"
            return Destination(lat, lon, label, fromAlias)
        }
    }
}
