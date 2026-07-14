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

// Radio (en grados) del sesgo de cercanía alrededor de la posición actual --
// nombres de calle comunes ("Belgrano", "San Martín") se repiten en casi todo
// pueblo argentino, así que sin esto el resultado más "importante" para
// Nominatim puede quedar a cientos de km de donde estás.
private const val NEARBY_BIAS_DEGREES = 2.0

private const val RESULT_LIMIT = 5

/**
 * Busqueda de direcciones via Nominatim (geocoder gratuito de OSM). Requiere
 * internet -- igual que la descarga de tiles de BRouter -- y respeta la
 * politica de uso de Nominatim: un solo request por busqueda (nada de
 * autocompletado por tecla) y un User-Agent identificable.
 *
 * Devuelve varios candidatos (no el primero a ciegas) -- un nombre de calle
 * ambiguo puede matchear muchos lugares distintos en el país, y hay que
 * dejar que la persona confirme cuál es antes de calcular una ruta.
 */
object GeocodingRepository {

    suspend fun search(query: String, near: Pair<Double, Double>? = null): List<GeocodeResult> =
        withContext(Dispatchers.IO) {
            val encoded = URLEncoder.encode(query, "UTF-8")
            // countrycodes=ar: la app es exclusivamente para Argentina (Res. 5/2015) --
            // sin esto, un nombre ambiguo ("Obelisco") puede matchear un lugar
            // homónimo en otro país antes que el de acá.
            var url = "$NOMINATIM_URL?q=$encoded&format=json&limit=$RESULT_LIMIT&countrycodes=ar"
            if (near != null) {
                val (lat, lon) = near
                val left = lon - NEARBY_BIAS_DEGREES
                val right = lon + NEARBY_BIAS_DEGREES
                val top = lat + NEARBY_BIAS_DEGREES
                val bottom = lat - NEARBY_BIAS_DEGREES
                // bounded=0 (default): la viewbox solo prioriza resultados cercanos,
                // no descarta el resto -- si buscás un destino lejos, sigue apareciendo.
                url += "&viewbox=$left,$top,$right,$bottom"
            }
            val connection = URL(url).openConnection() as HttpURLConnection
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
                (0 until results.length()).map { i ->
                    val r = results.getJSONObject(i)
                    GeocodeResult(
                        displayName = r.getString("display_name"),
                        lat = r.getString("lat").toDouble(),
                        lon = r.getString("lon").toDouble()
                    )
                }
            } finally {
                connection.disconnect()
            }
        }
}
