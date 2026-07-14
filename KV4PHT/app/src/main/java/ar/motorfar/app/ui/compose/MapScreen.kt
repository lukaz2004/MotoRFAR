package ar.motorfar.app.ui.compose

import android.Manifest
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import ar.motorfar.app.R
import ar.motorfar.app.nav.GeocodeResult
import ar.motorfar.app.nav.GeocodingException
import ar.motorfar.app.nav.GeocodingRepository
import ar.motorfar.app.nav.RouteCalculationException
import ar.motorfar.app.nav.RouteEngine
import ar.motorfar.app.nav.RouteTileException
import ar.motorfar.app.nav.RouteTileRepository
import ar.motorfar.app.ui.compose.state.GroupMember
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.MotoRFARColors
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File

// Obelisco de Buenos Aires — vista inicial del mapa
private val OBELISCO = GeoPoint(-34.6037, -58.3816)
private const val INITIAL_ZOOM = 15.0

@Composable
fun MapScreen(
    groupMembers: List<GroupMember>,
    routePoints: List<ar.motorfar.app.data.RoutePoint> = emptyList(),
    locationGranted: Boolean = false,
    headingDeg: Float? = null,
    focusTarget: Pair<Double, Double>? = null,
    onFocusConsumed: () -> Unit = {},
    isTransmitting: Boolean = false,
    listenOnly: Boolean = false,
    isEmergency: Boolean = false,
    onPttDown: () -> Unit = {},
    onPttUp: () -> Unit = {},
    // 2026-07-06: se movió acá desde la pantalla principal -- el usuario lo
    // marcó como fuera de lugar ahí ("es algo del mapa").
    onSendWaypoint: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalMotoRFARColors.current
    val accentArgb = colors.accent.toArgb()
    val isDark = colors.background.luminance() < 0.5f

    // Estado de la UI del mapa
    var routeOriented by remember { mutableStateOf(false) }  // mapa rota según el rumbo

    // HUD táctico: coordenadas y zoom del centro del mapa (en vivo)
    var hudLat  by remember { mutableStateOf(OBELISCO.latitude) }
    var hudLon  by remember { mutableStateOf(OBELISCO.longitude) }
    var hudZoom by remember { mutableStateOf(INITIAL_ZOOM) }

    // Punto de foco: ubicación de una alerta a la que se "fue" desde el chat
    var focusPoint by remember { mutableStateOf<GeoPoint?>(null) }

    // Navegación turn-by-turn (MVP): elegir destino tocando el mapa, calcular
    // ruta real con BRouter (ar.motorfar.app.nav) y dibujarla. Sin voz ni
    // recálculo automático -- ver _PROYECTO/NAV_TURN_BY_TURN_DISENO.md.
    var pickingDestination by remember { mutableStateOf(false) }
    var navRoute by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var navDistanceKm by remember { mutableStateOf<Float?>(null) }
    var navError by remember { mutableStateOf<String?>(null) }
    var isCalculatingRoute by remember { mutableStateOf(false) }
    var addressQuery by remember { mutableStateOf("") }
    var isSearchingAddress by remember { mutableStateOf(false) }
    var addressResults by remember { mutableStateOf<List<GeocodeResult>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

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

    fun calculateRouteTo(dest: GeoPoint) {
        val origin = myLocationOverlay.myLocation
        if (origin == null) {
            navError = "Sin fix GPS todavía -- esperá señal e intentá de nuevo."
            return
        }
        navError = null
        isCalculatingRoute = true
        coroutineScope.launch {
            try {
                val tileDir = File(context.filesDir, "brouter_tiles")
                RouteTileRepository.ensureTileDownloaded(origin.latitude, origin.longitude, tileDir)
                navRoute = RouteEngine.calculateRoute(context, origin, dest, tileDir)
                val results = FloatArray(1)
                var totalM = 0f
                for (i in 1 until navRoute.size) {
                    android.location.Location.distanceBetween(
                        navRoute[i - 1].latitude, navRoute[i - 1].longitude,
                        navRoute[i].latitude, navRoute[i].longitude,
                        results
                    )
                    totalM += results[0]
                }
                navDistanceKm = totalM / 1000f
            } catch (e: RouteTileException) {
                navError = e.message
                navRoute = emptyList()
            } catch (e: RouteCalculationException) {
                navError = e.message
                navRoute = emptyList()
            } finally {
                isCalculatingRoute = false
            }
        }
    }

    fun selectAddressResult(result: GeocodeResult) {
        addressResults = emptyList()
        val dest = GeoPoint(result.lat, result.lon)
        pickingDestination = false
        focusPoint = dest
        mapView.controller.animateTo(dest)
        mapView.controller.setZoom(16.0)
        calculateRouteTo(dest)
    }

    fun searchAddress() {
        val query = addressQuery.trim()
        if (query.isEmpty()) return
        navError = null
        addressResults = emptyList()
        isSearchingAddress = true
        coroutineScope.launch {
            try {
                val near = myLocationOverlay.myLocation?.let { it.latitude to it.longitude }
                val results = GeocodingRepository.search(query, near)
                if (results.isEmpty()) {
                    navError = "No se encontró esa dirección."
                } else {
                    // Siempre confirma con una lista, incluso con 1 solo resultado --
                    // "Belgrano" u otro nombre de calle común puede matchear un
                    // lugar lejos del que se busca, y hay que ver la localidad/
                    // provincia antes de calcular una ruta a ciegas.
                    addressResults = results
                }
            } catch (e: GeocodingException) {
                navError = e.message
            } finally {
                isSearchingAddress = false
            }
        }
    }

    val mapEventsOverlay = remember {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                if (pickingDestination) {
                    pickingDestination = false
                    focusPoint = p
                    calculateRouteTo(p)
                    return true
                }
                return false
            }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        })
    }

    DisposableEffect(Unit) {
        if (!mapView.overlays.contains(mapEventsOverlay)) {
            mapView.overlays.add(0, mapEventsOverlay)
        }
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
            // Arranca centrado en MI ubicación (no en el Obelisco) al primer fix de GPS
            myLocationOverlay.runOnFirstFix {
                myLocationOverlay.myLocation?.let { loc ->
                    mapView.post {
                        mapView.controller.animateTo(loc)
                        mapView.controller.setZoom(17.0)
                    }
                }
            }
            mapView.invalidate()
        }
    }

    // Orientación a la ruta: rota el mapa para que "arriba" sea hacia donde vas.
    // OSMDroid usa mapOrientation = -rumbo (el mapa gira opuesto al heading).
    androidx.compose.runtime.LaunchedEffect(routeOriented, headingDeg) {
        if (routeOriented && headingDeg != null) {
            mapView.mapOrientation = -headingDeg
        } else if (!routeOriented) {
            mapView.mapOrientation = 0f  // norte arriba
        }
        mapView.invalidate()
    }

    // Mapa oscuro/táctico en temas oscuros (sin filtro en tema Día claro)
    androidx.compose.runtime.LaunchedEffect(isDark) {
        mapView.overlayManager.tilesOverlay.setColorFilter(
            if (isDark) darkTacticalTileFilter() else null
        )
        mapView.invalidate()
    }

    // HUD: actualiza coordenadas/zoom al mover o hacer zoom en el mapa
    DisposableEffect(Unit) {
        fun refresh() {
            val c = mapView.mapCenter
            hudLat = c.latitude
            hudLon = c.longitude
            hudZoom = mapView.zoomLevelDouble
        }
        val listener = object : org.osmdroid.events.MapListener {
            override fun onScroll(e: org.osmdroid.events.ScrollEvent?): Boolean { refresh(); return false }
            override fun onZoom(e: org.osmdroid.events.ZoomEvent?): Boolean { refresh(); return false }
        }
        mapView.addMapListener(listener)
        refresh()
        onDispose { mapView.removeMapListener(listener) }
    }

    // Ir a la ubicación de una alerta: centra, marca el punto y "consume" el target
    androidx.compose.runtime.LaunchedEffect(focusTarget) {
        val t = focusTarget ?: return@LaunchedEffect
        val gp = GeoPoint(t.first, t.second)
        focusPoint = gp
        mapView.controller.animateTo(gp)
        mapView.controller.setZoom(17.5)
        mapView.invalidate()
        onFocusConsumed()
    }

    // Primer render en horizontal: el MapView llegaba a dibujarse fuera de sus límites y
    // tapaba la NavigationRail hasta el primer redraw. clipToBounds lo contiene y el nudge
    // fuerza un re-layout al entrar al mapa.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(60)
        mapView.requestLayout()
        mapView.invalidate()
    }

    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = { mv ->
                // Dibuja la traza de la ruta (Polyline)
                mv.overlays.removeAll { it is Polyline }
                if (routePoints.size > 1) {
                    val line = Polyline(mv).apply {
                        outlinePaint.color = accentArgb
                        outlinePaint.strokeWidth = 5f
                        setPoints(routePoints.map { GeoPoint(it.latitude, it.longitude) })
                    }
                    mv.overlays.add(line)
                }
                // Dibuja la ruta calculada por BRouter (turn-by-turn, MVP sin voz)
                if (navRoute.size > 1) {
                    val navLine = Polyline(mv).apply {
                        outlinePaint.color = android.graphics.Color.MAGENTA
                        outlinePaint.strokeWidth = 7f
                        setPoints(navRoute)
                    }
                    mv.overlays.add(navLine)
                }

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
                // Marcador de foco (ubicación de una alerta abierta desde el chat)
                focusPoint?.let { fp ->
                    val fm = Marker(mv).apply {
                        position = fp
                        title    = "Ubicación de alerta"
                        icon     = buildFocusMarker(mv.context, accentArgb)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    mv.overlays.add(fm)
                }
                mv.invalidate()
            }
        )

        // HUD táctico: coordenadas + zoom + rumbo, con los controles de mapa al lado
        // (2026-07-06: se sacaron los botones +/- de zoom -- el pellizco con dos dedos
        // ya hace zoom, molestaban duplicados en pantalla -- y orientación/centrado se
        // movieron acá arriba para que el PTT quede solo abajo, sin riesgo de toque
        // accidental con botones chicos al lado mientras se maneja).
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color  = colors.background.copy(alpha = 0.78f),
                shape  = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, colors.borderSubtle)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    // 2026-07-08: lat/lon apiladas en vez de una sola línea ancha --
                    // en vertical ese ancho no dejaba lugar para el botón WAYPOINT,
                    // que terminaba fuera de pantalla.
                    androidx.compose.material3.Text(
                        text       = hudLatLine(hudLat),
                        color      = colors.textPrimary,
                        fontFamily = ar.motorfar.app.ui.compose.theme.ShareTechMono,
                        fontSize   = 15.sp
                    )
                    androidx.compose.material3.Text(
                        text       = hudLonLine(hudLon),
                        color      = colors.textPrimary,
                        fontFamily = ar.motorfar.app.ui.compose.theme.ShareTechMono,
                        fontSize   = 15.sp
                    )
                    androidx.compose.material3.Text(
                        text       = "ZM " + hudZoom.toInt() + (headingDeg?.let { "   ·   HDG " + (((it % 360) + 360) % 360).toInt() + "°" } ?: ""),
                        color      = colors.textSecondary,
                        fontFamily = ar.motorfar.app.ui.compose.theme.ShareTechMono,
                        fontSize   = 13.sp
                    )
                }
            }

            // Orientar a la ruta (rota el mapa según el rumbo)
            MapControlButton(
                iconRes = R.drawable.ic_pin,
                label   = "RUMBO",
                colors  = colors,
                active  = routeOriented,
                onClick = {
                    routeOriented = !routeOriented
                    if (routeOriented && headingDeg != null) {
                        mapView.mapOrientation = -headingDeg
                    } else {
                        mapView.mapOrientation = 0f
                    }
                    mapView.invalidate()
                }
            )
            // Centrar en mi ubicación
            MapControlButton(
                iconRes = R.drawable.ic_pin,
                label   = "MI GPS",
                colors  = colors,
                accent  = true,
                onClick = {
                    val loc = myLocationOverlay.myLocation
                    if (loc != null) {
                        mapView.controller.animateTo(loc)
                        mapView.controller.setZoom(17.0)
                    } else {
                        mapView.controller.animateTo(OBELISCO)
                        mapView.controller.setZoom(INITIAL_ZOOM)
                    }
                }
            )
            // Enviar waypoint: transmite tu posición GPS al grupo en un toque
            MapControlButton(
                iconRes = R.drawable.ic_send,
                label   = "WAYPOINT",
                colors  = colors,
                onClick = onSendWaypoint
            )
            // Navegación: tocar el mapa elige destino y calcula la ruta (BRouter, offline)
            MapControlButton(
                iconRes = R.drawable.ic_pin,
                label   = if (pickingDestination) "TOCÁ EL MAPA" else "IR A",
                colors  = colors,
                active  = pickingDestination,
                onClick = {
                    if (navRoute.isNotEmpty()) {
                        navRoute = emptyList()
                        navDistanceKm = null
                        navError = null
                    } else {
                        pickingDestination = !pickingDestination
                        addressResults = emptyList()
                    }
                }
            )
        }

        // Buscador de direcciones (Nominatim/OSM): alternativa a tocar el mapa
        // para elegir destino -- requiere internet en el momento de buscar.
        if (pickingDestination) {
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(top = 76.dp, start = 12.dp, end = 12.dp),
                color    = colors.background.copy(alpha = 0.85f),
                shape    = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                border   = BorderStroke(1.dp, colors.borderSubtle)
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    androidx.compose.material3.TextField(
                        value = addressQuery,
                        onValueChange = { addressQuery = it },
                        placeholder = { androidx.compose.material3.Text("Dirección o lugar...", fontSize = 13.sp) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = { searchAddress() }
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape    = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                        color    = colors.accent.copy(alpha = 0.18f),
                        border   = BorderStroke(1.dp, colors.accent),
                        modifier = Modifier.clickable(onClick = ::searchAddress)
                    ) {
                        androidx.compose.material3.Text(
                            text = if (isSearchingAddress) "..." else "BUSCAR",
                            color = colors.accent,
                            fontFamily = ar.motorfar.app.ui.compose.theme.ShareTechMono,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }

        // Candidatos del buscador: hay que confirmar cuál es antes de trazar
        // una ruta -- un nombre de calle común ("Belgrano") matchea decenas de
        // lugares distintos en el país, y elegir el primero a ciegas podría
        // mandar a cientos de km del destino real.
        if (addressResults.isNotEmpty()) {
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(top = 132.dp, start = 12.dp, end = 12.dp),
                color    = colors.background.copy(alpha = 0.92f),
                shape    = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                border   = BorderStroke(1.dp, colors.accent)
            ) {
                Column {
                    addressResults.forEach { result ->
                        androidx.compose.material3.Text(
                            text = result.displayName,
                            color = colors.textPrimary,
                            fontFamily = ar.motorfar.app.ui.compose.theme.ShareTechMono,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectAddressResult(result) }
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        // Estado de la navegación calculada (distancia, calculando, o error)
        if (isCalculatingRoute || navError != null || navDistanceKm != null) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                color    = colors.background.copy(alpha = 0.85f),
                shape    = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                border   = BorderStroke(1.dp, if (navError != null) ar.motorfar.app.ui.compose.theme.EmergencyBorder else colors.borderActive)
            ) {
                androidx.compose.material3.Text(
                    text = when {
                        isCalculatingRoute -> "Calculando ruta…"
                        navError != null   -> navError!!
                        else               -> "Ruta: %.1f km".format(navDistanceKm)
                    },
                    color      = if (navError != null) ar.motorfar.app.ui.compose.theme.EmergencyBorder else colors.textPrimary,
                    fontFamily = ar.motorfar.app.ui.compose.theme.ShareTechMono,
                    fontSize   = 14.sp,
                    modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // PTT directo desde el mapa, solo en su esquina -- sin botones chicos al lado
        // para evitar toques accidentales manejando (el resto de los controles se
        // movió arriba, junto al HUD de coordenadas).
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            MapPttButton(
                colors         = colors,
                isTransmitting = isTransmitting,
                listenOnly     = listenOnly,
                onPttDown      = onPttDown,
                onPttUp        = onPttUp,
                isEmergency    = isEmergency
            )
        }

        // Indicador de modo "orientado a ruta"
        if (routeOriented) {
            Surface(
                color    = colors.surface.copy(alpha = 0.85f),
                shape    = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            ) {
                androidx.compose.material3.Text(
                    text     = "▲ RUTA ARRIBA",
                    color    = colors.accent,
                    fontFamily = ar.motorfar.app.ui.compose.theme.ShareTechMono,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/** Botón de control del mapa: ícono + etiqueta corta (orientación, mi ubicación). */
@Composable
private fun MapControlButton(
    iconRes: Int,
    label: String,
    colors: MotoRFARColors,
    onClick: () -> Unit,
    active: Boolean = false,
    accent: Boolean = false
) {
    val tint = when {
        accent -> colors.accent
        active -> colors.accent
        else   -> colors.textSecondary
    }
    Surface(
        shape    = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        color    = if (active) colors.accent.copy(alpha = 0.18f) else colors.surface,
        border   = androidx.compose.foundation.BorderStroke(
            1.dp, if (active) colors.accent else colors.borderSubtle
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter            = painterResource(iconRes),
                contentDescription = label,
                tint               = tint,
                modifier           = Modifier.size(22.dp)
            )
            androidx.compose.material3.Text(
                text       = label,
                color      = tint,
                fontFamily = ar.motorfar.app.ui.compose.theme.ShareTechMono,
                fontSize   = 10.sp
            )
        }
    }
}

/**
 * Botón de PTT compacto para la pantalla de mapa. Mantener presionado transmite
 * (key-down → key-up al soltar), igual que el PTT principal. Muestra "TX" relleno
 * de acento mientras transmite. En modo escucha igual reporta el bloqueo (Toast).
 */
@Composable
private fun MapPttButton(
    colors: MotoRFARColors,
    isTransmitting: Boolean,
    listenOnly: Boolean,
    onPttDown: () -> Unit,
    onPttUp: () -> Unit,
    // 2026-07-08: mismo look que el PTT de la pantalla principal (gradiente +
    // anillos de TX vía PttVisual) -- antes era un círculo plano distinto,
    // el usuario lo marcó como inconsistente entre pantallas.
    isEmergency: Boolean = false
) {
    val haptics = LocalHapticFeedback.current
    val accentColor = when {
        isEmergency -> ar.motorfar.app.ui.compose.theme.EmergencyBorder
        listenOnly  -> colors.textDisabled
        else        -> colors.accent
    }
    val label = if (isTransmitting) "TRANSMITIENDO" else "PUSH TO TALK"

    ar.motorfar.app.ui.compose.components.PttVisual(
        // 2026-07-08: mismo diámetro que el PTT de la pantalla principal en
        // vertical (180dp) -- antes quedaba visiblemente más chico acá.
        diameter       = 180.dp,
        accentColor    = accentColor,
        isTransmitting = isTransmitting,
        label          = label,
        // 2026-07-07: el tap siempre dispara (a diferencia del PTT principal, que
        // se deshabilita entero) -- en modo escucha el caller igual necesita el
        // evento para avisar con un Toast que el PTT está bloqueado.
        modifier = Modifier.pointerInput(listenOnly) {
            detectTapGestures(
                onPress = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPttDown()
                    tryAwaitRelease()
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPttUp()
                }
            )
        }
    )
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

/** Filtro de color que oscurece los tiles claros de OSM (invierte + desatura). */
private fun darkTacticalTileFilter(): android.graphics.ColorMatrixColorFilter {
    val invert = android.graphics.ColorMatrix(floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f
    ))
    val desat = android.graphics.ColorMatrix().apply { setSaturation(0.35f) }
    invert.postConcat(desat)
    return android.graphics.ColorMatrixColorFilter(invert)
}

/** Formatea la latitud para el HUD: "S 34.6037°". */
private fun hudLatLine(lat: Double): String {
    val ns = if (lat >= 0) "N" else "S"
    return "%s %.4f°".format(ns, kotlin.math.abs(lat))
}

/** Formatea la longitud para el HUD: "O 58.3816°". */
private fun hudLonLine(lon: Double): String {
    val eo = if (lon >= 0) "E" else "O"
    return "%s %.4f\u00b0".format(eo, kotlin.math.abs(lon))
}

/** Marcador de "foco": un blanco/crosshair para la ubicación de una alerta. */
private fun buildFocusMarker(context: android.content.Context, accent: Int): android.graphics.drawable.Drawable {
    val density = context.resources.displayMetrics.density
    fun dp(v: Float) = v * density
    val r = dp(18f)
    val size = (r * 2 + dp(10f)).toInt()
    val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val cx = size / 2f
    val cy = size / 2f
    // Halo translúcido
    val halo = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = (accent and 0x00FFFFFF) or (45 shl 24)
    }
    canvas.drawCircle(cx, cy, r, halo)
    // Anillo
    val ring = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = dp(2.5f)
        color = accent
    }
    canvas.drawCircle(cx, cy, r, ring)
    // Cruz (crosshair) que sobresale del anillo
    val cross = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dp(2f)
        color = accent
    }
    canvas.drawLine(cx, cy - r - dp(4f), cx, cy - r + dp(7f), cross)
    canvas.drawLine(cx, cy + r - dp(7f), cx, cy + r + dp(4f), cross)
    canvas.drawLine(cx - r - dp(4f), cy, cx - r + dp(7f), cy, cross)
    canvas.drawLine(cx + r - dp(7f), cy, cx + r + dp(4f), cy, cross)
    // Punto central
    val dot = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = accent }
    canvas.drawCircle(cx, cy, dp(3f), dot)
    return android.graphics.drawable.BitmapDrawable(context.resources, bmp)
}
