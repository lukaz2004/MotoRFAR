package ar.motorfar.app.nav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.floor

private const val SEGMENTS_BASE_URL = "https://brouter.de/brouter/segments4/"

// Los .rd5 reales pesan varios MB -- un archivo mas chico que esto es un
// error/pagina de error, no datos de ruteo validos.
private const val MIN_VALID_TILE_BYTES = 100_000L

class RouteTileException(message: String) : Exception(message)

/** Descarga bajo demanda el tile .rd5 de BRouter (grilla de 5x5 grados) que cubre un punto. */
object RouteTileRepository {

    /** Compartido con CountryTileRepository -- mismo directorio para que la descarga masiva y la bajo-demanda se vean entre sí. */
    const val TILE_DIR_NAME = "brouter_tiles"

    /** Mismo cálculo que btools.mapaccess.NodesCache.fileForSegment(), verificado contra la fuente vendorizada. */
    fun tileNameFor(lat: Double, lon: Double): String {
        val tileLon = (floor(lon / 5.0) * 5).toInt()
        val tileLat = (floor(lat / 5.0) * 5).toInt()
        val lonStr = if (tileLon < 0) "W${-tileLon}" else "E$tileLon"
        val latStr = if (tileLat < 0) "S${-tileLat}" else "N$tileLat"
        return "${lonStr}_${latStr}.rd5"
    }

    fun localFileFor(lat: Double, lon: Double, destDir: File): File =
        File(destDir, tileNameFor(lat, lon))

    fun isDownloaded(lat: Double, lon: Double, destDir: File): Boolean =
        localFileFor(lat, lon, destDir).exists()

    suspend fun ensureTileDownloaded(lat: Double, lon: Double, destDir: File): File =
        withContext(Dispatchers.IO) {
            destDir.mkdirs()
            val dest = localFileFor(lat, lon, destDir)
            if (dest.exists()) return@withContext dest

            val connection = URL(SEGMENTS_BASE_URL + dest.name).openConnection() as HttpURLConnection
            try {
                try {
                    connection.connect()
                } catch (e: java.io.IOException) {
                    throw RouteTileException("No se pudo conectar para descargar datos de ruteo: ${e.message}")
                }
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw RouteTileException("No hay datos de ruteo para esta zona (${dest.name}).")
                }
                try {
                    connection.inputStream.use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    }
                } catch (e: java.io.IOException) {
                    dest.delete()
                    throw RouteTileException("Falló la descarga de datos de ruteo: ${e.message}")
                }
            } finally {
                connection.disconnect()
            }

            if (dest.length() < MIN_VALID_TILE_BYTES) {
                dest.delete()
                throw RouteTileException("La descarga de datos de ruteo (${dest.name}) parece incompleta.")
            }
            dest
        }
}
