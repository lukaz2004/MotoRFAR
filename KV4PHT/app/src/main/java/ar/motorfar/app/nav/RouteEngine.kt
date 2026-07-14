package ar.motorfar.app.nav

import android.content.Context
import btools.router.OsmNodeNamed
import btools.router.ProfileCache
import btools.router.RoutingContext
import btools.router.RoutingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.io.File

class RouteCalculationException(message: String) : Exception(message)

/**
 * Wrapper sobre btools.router.RoutingEngine (BRouter vendorizado, ver
 * KV4PHT/brouter-core). Perfil unico "trekking" (generico para
 * moto/4x4/bici/a pie, bundleado como asset -- ver ProfileCache.parseProfile
 * en la fuente vendorizada para el porque de este setup).
 */
object RouteEngine {

    private const val PROFILE_NAME = "trekking"
    private const val PROFILE_ASSET_DIR = "brouter_profile"

    private fun ensureProfileExtracted(context: Context): File {
        val destDir = File(context.filesDir, PROFILE_ASSET_DIR)
        val brf = File(destDir, "$PROFILE_NAME.brf")
        val lookups = File(destDir, "lookups.dat")
        if (brf.exists() && lookups.exists()) return destDir

        destDir.mkdirs()
        context.assets.open("$PROFILE_ASSET_DIR/$PROFILE_NAME.brf").use { input ->
            brf.outputStream().use { output -> input.copyTo(output) }
        }
        context.assets.open("$PROFILE_ASSET_DIR/lookups.dat").use { input ->
            lookups.outputStream().use { output -> input.copyTo(output) }
        }
        return destDir
    }

    private fun toOsmNode(point: GeoPoint, name: String): OsmNodeNamed {
        val node = OsmNodeNamed()
        node.name = name
        node.ilon = ((point.longitude + 180.0) * 1_000_000.0).toInt()
        node.ilat = ((point.latitude + 90.0) * 1_000_000.0).toInt()
        return node
    }

    suspend fun calculateRoute(context: Context, from: GeoPoint, to: GeoPoint, tileDir: File): List<GeoPoint> =
        withContext(Dispatchers.Default) {
            val profileDir = ensureProfileExtracted(context)
            val rc = RoutingContext()
            rc.localFunction = PROFILE_NAME
            System.setProperty("profileBaseDir", profileDir.absolutePath)
            ProfileCache.parseProfile(rc)
            try {
                val waypoints = listOf(toOsmNode(from, "from"), toOsmNode(to, "to"))
                val engine = RoutingEngine(null, null, tileDir, waypoints, rc)
                engine.doRun(0)

                val error = engine.errorMessage
                if (error != null) {
                    throw RouteCalculationException(error)
                }
                val track = engine.foundTrack
                if (track == null || track.nodes.isEmpty()) {
                    throw RouteCalculationException("No se pudo calcular una ruta a este destino.")
                }
                track.nodes.map { node ->
                    GeoPoint(node.getILat() / 1_000_000.0 - 90.0, node.getILon() / 1_000_000.0 - 180.0)
                }
            } finally {
                ProfileCache.releaseProfile(rc)
            }
        }
}
