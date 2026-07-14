package ar.motorfar.app.nav

import android.content.Context
import btools.router.OsmNodeNamed
import btools.router.ProfileCache
import btools.router.RoutingContext
import btools.router.RoutingEngine
import btools.router.VoiceHint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.io.File

class RouteCalculationException(message: String) : Exception(message)

/** Un giro a señalizar en el HUD -- command usa las constantes de btools.router.VoiceHint (TL, TR, RNDB, etc). */
data class TurnHint(val command: Int, val roundaboutExit: Int, val point: GeoPoint, val trackIndex: Int)

data class RouteResult(val points: List<GeoPoint>, val turnHints: List<TurnHint>)

/**
 * Wrapper sobre btools.router.RoutingEngine (BRouter vendorizado, ver
 * KV4PHT/brouter-core). Perfil unico "trekking" (generico para
 * moto/4x4/bici/a pie, bundleado como asset -- ver ProfileCache.parseProfile
 * en la fuente vendorizada para el porque de este setup).
 */
object RouteEngine {

    // car-vario respeta el sentido de circulación como una restricción dura
    // (onewayspeedlimit=0 en contramano) -- correcto para moto/4x4, a
    // diferencia de "trekking" (penaliza pero permite ir en contramano,
    // pensado para bici/a pie donde eso es legal). El proyecto es
    // multi-modal (moto/4x4/bici/senderismo), pero default seguro: el error
    // de sugerirle a un vehículo ir en contramano es peligroso e ilegal; el
    // error de ser demasiado estricto para un peatón no lo es.
    private const val PROFILE_NAME = "car-vario"
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

    suspend fun calculateRoute(context: Context, from: GeoPoint, to: GeoPoint, tileDir: File): RouteResult =
        withContext(Dispatchers.Default) {
            val profileDir = ensureProfileExtracted(context)
            val rc = RoutingContext()
            rc.localFunction = PROFILE_NAME
            System.setProperty("profileBaseDir", profileDir.absolutePath)
            ProfileCache.parseProfile(rc)
            // 1=auto -- el perfil no define turnInstructionMode, así que parseProfile
            // lo deja en 0 (default) y BRouter nunca registra los datos de giro
            // alternativo que necesita processVoiceHints() para calcular comandos
            // reales (queda con voiceHints.list vacío). Hay que pisarlo DESPUÉS de
            // parseProfile, no antes -- parseProfile es quien lo resetea a 0.
            rc.turnInstructionMode = 1
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
                val points = track.nodes.map { node ->
                    GeoPoint(node.getILat() / 1_000_000.0 - 90.0, node.getILon() / 1_000_000.0 - 180.0)
                }
                // END (llegada) y BL (beeline, tramo sin datos de ruteo) no son
                // giros a señalizar en el HUD.
                val turnHints = track.voiceHints?.list.orEmpty()
                    .filter { it.cmd != VoiceHint.END && it.cmd != VoiceHint.BL }
                    .map { hint ->
                        TurnHint(
                            command = hint.cmd,
                            roundaboutExit = hint.getExitNumber(),
                            point = GeoPoint(hint.ilat / 1_000_000.0 - 90.0, hint.ilon / 1_000_000.0 - 180.0),
                            trackIndex = hint.indexInTrack
                        )
                    }
                RouteResult(points, turnHints)
            } finally {
                ProfileCache.releaseProfile(rc)
            }
        }
}
