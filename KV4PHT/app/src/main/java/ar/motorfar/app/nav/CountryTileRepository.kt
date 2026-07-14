package ar.motorfar.app.nav

import java.io.File
import kotlin.math.floor

// Bounding box rectangular de Argentina, con margen (incluye borde de Chile/
// Uruguay/Paraguay/Brasil) -- simplificación deliberada: recortar al
// contorno real del país para ahorrar un puñado de tiles oceánicos no vale
// la complejidad extra, y los tiles de más simplemente se saltan si no hay
// datos (ver downloadAll).
private const val AR_LAT_MIN = -56.0
private const val AR_LAT_MAX = -21.0
private const val AR_LON_MIN = -74.0
private const val AR_LON_MAX = -53.0
private const val TILE_SIZE_DEGREES = 5.0

data class CountryDownloadProgress(
    val completed: Int,
    val total: Int,
    val currentTile: String?,
    val failedTiles: List<String>
)

/** Descarga en lote los tiles .rd5 de BRouter que cubren Argentina, para rutear sin conexión en cualquier parte del país. */
object CountryTileRepository {

    /** Punto central de cada celda de 5x5 grados que cubre (con margen) el territorio argentino. */
    fun argentinaTileCenters(): List<Pair<Double, Double>> {
        val centers = mutableListOf<Pair<Double, Double>>()
        var lat = floor(AR_LAT_MIN / TILE_SIZE_DEGREES) * TILE_SIZE_DEGREES
        while (lat < AR_LAT_MAX) {
            var lon = floor(AR_LON_MIN / TILE_SIZE_DEGREES) * TILE_SIZE_DEGREES
            while (lon < AR_LON_MAX) {
                centers.add((lat + TILE_SIZE_DEGREES / 2) to (lon + TILE_SIZE_DEGREES / 2))
                lon += TILE_SIZE_DEGREES
            }
            lat += TILE_SIZE_DEGREES
        }
        return centers
    }

    /**
     * Descarga secuencial de todos los tiles que cubren Argentina. Cada tile
     * fallido (celda oceánica sin datos, corte momentáneo de conexión) se
     * salta y se registra en `failedTiles` -- no aborta el resto del lote.
     * Idempotente: los tiles ya bajados (por esta función o por el uso
     * normal bajo demanda) se saltan sin volver a pedirlos.
     */
    suspend fun downloadAll(destDir: File, onProgress: (CountryDownloadProgress) -> Unit) {
        val centers = argentinaTileCenters()
        val failed = mutableListOf<String>()
        centers.forEachIndexed { index, (lat, lon) ->
            val name = RouteTileRepository.tileNameFor(lat, lon)
            onProgress(CountryDownloadProgress(index, centers.size, name, failed.toList()))
            if (!RouteTileRepository.isDownloaded(lat, lon, destDir)) {
                try {
                    RouteTileRepository.ensureTileDownloaded(lat, lon, destDir)
                } catch (e: RouteTileException) {
                    failed.add(name)
                }
            }
        }
        onProgress(CountryDownloadProgress(centers.size, centers.size, null, failed.toList()))
    }
}
