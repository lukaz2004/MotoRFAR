package ar.motorfar.app.maps

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

private const val MANIFEST_URL =
    "https://raw.githubusercontent.com/lukaz2004/MotoRFAR/main/_PROYECTO/mapas_offline/provincias.json"

data class ProvinceMapInfo(
    val iso: String,
    val name: String,
    val downloadUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val assetFilename: String
)

class ProvinceMapDownloadException(message: String) : Exception(message)

object ProvinceMapRepository {

    /** Bajado en vivo desde el repo -- evita rebundlear el manifest en el APK cada vez que se agrega una provincia. */
    suspend fun fetchManifest(): List<ProvinceMapInfo> = withContext(Dispatchers.IO) {
        val json = URL(MANIFEST_URL).openStream().bufferedReader().use { it.readText() }
        val provinces = org.json.JSONObject(json).getJSONArray("provinces")
        parseProvinces(provinces)
    }

    private fun parseProvinces(provinces: JSONArray): List<ProvinceMapInfo> =
        (0 until provinces.length()).map { i ->
            val p = provinces.getJSONObject(i)
            ProvinceMapInfo(
                iso = p.getString("iso3166_2"),
                name = p.getString("name"),
                downloadUrl = p.getString("download_url"),
                // El manifest lo guarda como "sha256:<hex>" -- se saca el prefijo para comparar contra MessageDigest.
                sha256 = p.getString("sha256").removePrefix("sha256:"),
                sizeBytes = p.getLong("size_bytes"),
                assetFilename = p.getString("asset_filename")
            )
        }

    fun localFileFor(info: ProvinceMapInfo, destDir: File): File = File(destDir, "${info.iso}.map")

    fun isDownloaded(info: ProvinceMapInfo, destDir: File): Boolean = localFileFor(info, destDir).exists()

    /**
     * Descarga con verificacion de integridad -- si el SHA-256 no matchea el
     * del manifest, borra el archivo parcial/corrupto y tira excepcion en vez
     * de dejar un .map invalido que despues rompa el renderer de Mapsforge.
     */
    suspend fun downloadProvince(
        info: ProvinceMapInfo,
        destDir: File,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        destDir.mkdirs()
        val dest = localFileFor(info, destDir)
        val digest = MessageDigest.getInstance("SHA-256")
        val connection = URL(info.downloadUrl).openConnection() as HttpURLConnection
        try {
            connection.connect()
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: info.sizeBytes
            connection.inputStream.use { input ->
                FileOutputStream(dest).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        downloaded += read
                        if (total > 0) onProgress(downloaded.toFloat() / total)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actualHash.equals(info.sha256, ignoreCase = true)) {
            dest.delete()
            throw ProvinceMapDownloadException(
                "Hash no coincide para ${info.name}: esperado ${info.sha256}, obtenido $actualHash"
            )
        }
        dest
    }
}
