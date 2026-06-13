package ar.motorfar.app.ui.compose

import android.Manifest
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import ar.motorfar.app.R
import ar.motorfar.app.ui.compose.state.GroupMember
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File

// Obelisco de Buenos Aires — vista inicial del mapa
private val OBELISCO = GeoPoint(-34.6037, -58.3816)
private const val INITIAL_ZOOM = 15.0

@Composable
fun MapScreen(
    groupMembers: List<GroupMember>,
    locationGranted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalMotoRFARColors.current
    val accentArgb = colors.accent.toArgb()

    val hasLocationPermission = locationGranted || remember {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // MapView persistente entre recomposiciones
    val mapView = remember {
        Configuration.getInstance().apply {
            load(context, PreferenceManager.getDefaultSharedPreferences(context))
            // User-Agent obligatorio para el servidor de tiles de OSM
            userAgentValue = context.packageName
            osmdroidTileCache = File(context.cacheDir, "osm_tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            controller.setZoom(INITIAL_ZOOM)
            controller.setCenter(OBELISCO)
        }
    }

    // Overlay de mi ubicación (punto azul + seguimiento)
    val myLocationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            if (hasLocationPermission) enableMyLocation()
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            myLocationOverlay.disableMyLocation()
            mapView.onPause()
        }
    }

    // Activa la capa de ubicación cuando el permiso está disponible
    androidx.compose.runtime.LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            myLocationOverlay.enableMyLocation()
            if (!mapView.overlays.contains(myLocationOverlay)) {
                mapView.overlays.add(myLocationOverlay)
            }
            mapView.invalidate()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = { mv ->
                // Refresca marcadores de miembros del grupo con etiqueta táctica
                mv.overlays.removeAll { it is Marker }
                val nowMs = System.currentTimeMillis()
                groupMembers.forEach { member ->
                    val stale = member.isStale(nowMs)
                    val marker = Marker(mv).apply {
                        position = GeoPoint(member.lat, member.lon)
                        title    = member.alias
                        snippet  = if (member.distanceM > 0) "${member.distanceM} m" else "en posición"
                        icon     = buildGroupMarker(
                            context  = mv.context,
                            label    = member.alias,
                            accent   = accentArgb,
                            stale    = stale
                        )
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    mv.overlays.add(marker)
                }
                mv.invalidate()
            }
        )

        // Botón "centrar en mi ubicación"
        Surface(
            shape    = CircleShape,
            color    = colors.surface,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(48.dp)
                .clickable {
                    val loc = myLocationOverlay.myLocation
                    if (loc != null) {
                        mapView.controller.animateTo(loc)
                        mapView.controller.setZoom(17.0)
                    } else {
                        // Sin fix GPS aún: vuelve al Obelisco
                        mapView.controller.animateTo(OBELISCO)
                        mapView.controller.setZoom(INITIAL_ZOOM)
                    }
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter            = painterResource(R.drawable.ic_pin),
                    contentDescription = "Mi ubicación",
                    tint               = colors.accent
                )
            }
        }
    }
}

/**
 * Dibuja un marcador táctico para un integrante del grupo:
 * un pin redondeado con la inicial del alias y una etiqueta debajo.
 * Si [stale] es true (sin beacon reciente) se atenúa.
 */
private fun buildGroupMarker(
    context: android.content.Context,
    label: String,
    accent: Int,
    stale: Boolean
): android.graphics.drawable.Drawable {
    val density = context.resources.displayMetrics.density
    fun dp(v: Float) = v * density

    val pinR    = dp(16f)            // radio del círculo
    val labelH  = dp(16f)            // alto de la etiqueta de texto
    val padding = dp(4f)
    val w       = (pinR * 2 + dp(40f)).toInt()
    val h       = (pinR * 2 + labelH + padding).toInt()

    val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val cx = w / 2f
    val cy = pinR + dp(1f)

    val alpha = if (stale) 110 else 255
    fun withAlpha(c: Int) = (c and 0x00FFFFFF) or (alpha shl 24)

    // Halo
    val halo = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = withAlpha(accent)
        this.alpha = if (stale) 30 else 60
    }
    canvas.drawCircle(cx, cy, pinR + dp(3f), halo)

    // Círculo relleno oscuro con borde de acento
    val fill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(if (stale) 160 else 230, 8, 12, 8)
    }
    canvas.drawCircle(cx, cy, pinR, fill)

    val ring = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = dp(2f)
        color = withAlpha(accent)
    }
    canvas.drawCircle(cx, cy, pinR, ring)

    // Inicial del alias en el centro
    val initial = label.trim().take(1).uppercase().ifEmpty { "?" }
    val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = withAlpha(accent)
        textSize = dp(16f)
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        typeface = android.graphics.Typeface.MONOSPACE
    }
    val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(initial, cx, textY, textPaint)

    // Etiqueta con el alias debajo del pin
    val labelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = withAlpha(accent)
        textSize = dp(11f)
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.MONOSPACE
    }
    canvas.drawText(label.uppercase(), cx, cy + pinR + dp(12f), labelPaint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bmp)
}
