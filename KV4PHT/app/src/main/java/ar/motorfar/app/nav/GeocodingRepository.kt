package ar.motorfar.app.nav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val NOMINATIM_URL = "https://nominatim.openstreetmap.org/search"

class GeocodingException(message: String) : Exception(message)

data class GeocodeResult(val displayName: String, val lat: Double, val lon: Double)

/**
 * Busqueda de direcciones via Nominatim (geocoder gratuito de OSM). Requiere
 * internet -- igual que la descarga de tiles de BRouter -- y respeta la
 * politica de uso de Nominatim: un solo request por busqueda (nada de
 * autocompletado por tecla) y un User-Agent identificable.
 */
object GeocodingRepository {

    suspend fun search(query: String): GeocodeResult? = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        // countrycodes=ar: la app es exclusivamente para Argentina (Res. 5/2015) --
        // sin esto, un nombre ambiguo ("Obelisco") puede matchear un lugar
        // homónimo en otro país antes que el de acá.
        val url = URL("$NOMINATIM_URL?q=$encoded&format=json&limit=1&countrycodes=ar")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.setRequestProperty("User-Agent", "MotoRFAR-Baqueano")
            try {
                connection.connect()
            } catch (e: java.io.IOException) {
                throw GeocodingException("No se pudo conectar para buscar la dirección: ${e.message}")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw GeocodingException("Falló la búsqueda de dirección (${connection.responseCode}).")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val results = JSONArray(body)
            if (results.length() == 0) return@withContext null
            val first = results.getJSONObject(0)
            GeocodeResult(
                displayName = first.getString("display_name"),
                lat = first.getString("lat").toDouble(),
                lon = first.getString("lon").toDouble()
            )
        } finally {
            connection.disconnect()
        }
    }
}
