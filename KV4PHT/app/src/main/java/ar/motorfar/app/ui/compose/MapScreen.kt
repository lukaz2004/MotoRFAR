package ar.motorfar.app.ui.compose

import android.preference.PreferenceManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import ar.motorfar.app.ui.compose.state.GroupMember
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

@Composable
fun MapScreen(
    groupMembers: List<GroupMember>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            Configuration.getInstance().apply {
                load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
                osmdroidTileCache = File(ctx.cacheDir, "osm_tiles")
            }
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                controller.setZoom(14.0)
                setMultiTouchControls(true)
                setBuiltInZoomControls(false)
            }
        },
        update = { mapView ->
            mapView.overlays.removeAll { it is Marker }
            groupMembers.forEach { member ->
                val marker = Marker(mapView).apply {
                    position = GeoPoint(member.lat, member.lon)
                    title    = member.alias
                }
                mapView.overlays.add(marker)
            }
            mapView.invalidate()
        }
    )
}
