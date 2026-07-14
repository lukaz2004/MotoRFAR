package ar.motorfar.app.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import ar.motorfar.app.maps.ProvinceMapInfo
import ar.motorfar.app.maps.ProvinceMapRepository
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.ShareTechMono
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import java.io.File

private var graphicFactoryInitialized = false

private fun ensureGraphicFactory(context: android.content.Context) {
    if (!graphicFactoryInitialized) {
        AndroidGraphicFactory.createInstance(context.applicationContext)
        graphicFactoryInitialized = true
    }
}

private sealed class DownloadState {
    object NotDownloaded : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    object Downloaded : DownloadState()
    data class Failed(val message: String) : DownloadState()
}

/**
 * Mapas offline vectoriales (Mapsforge) por provincia -- pantalla separada del
 * mapa en vivo (`MapScreen.kt`, OSMDroid). Motores incompatibles entre si, no
 * se mezclan en la misma vista.
 */
@Composable
fun OfflineVectorMapsScreen(onBack: () -> Unit = {}) {
    val colors = LocalMotoRFARColors.current
    val context = LocalContext.current
    val destDir = remember { File(context.filesDir, "mapsforge") }

    var provinces by remember { mutableStateOf<List<ProvinceMapInfo>?>(null) }
    var manifestError by remember { mutableStateOf<String?>(null) }
    var states by remember { mutableStateOf(mapOf<String, DownloadState>()) }
    var viewingProvince by remember { mutableStateOf<ProvinceMapInfo?>(null) }

    LaunchedEffect(Unit) {
        try {
            val fetched = ProvinceMapRepository.fetchManifest()
            provinces = fetched
            states = fetched.associate { info ->
                info.iso to if (ProvinceMapRepository.isDownloaded(info, destDir)) {
                    DownloadState.Downloaded
                } else {
                    DownloadState.NotDownloaded
                }
            }
        } catch (e: Exception) {
            manifestError = "No se pudo cargar la lista de provincias -- revisá tu conexión a internet."
        }
    }

    val viewing = viewingProvince
    if (viewing != null) {
        MapsforgeViewerScreen(
            mapFile = ProvinceMapRepository.localFileFor(viewing, destDir),
            title = viewing.name,
            onBack = { viewingProvince = null }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "‹ Volver",
            color = colors.textSecondary,
            fontFamily = ShareTechMono,
            fontSize = 15.sp,
            modifier = Modifier.clickable(onClick = onBack)
        )
        Text(
            text = "MAPAS OFFLINE",
            color = colors.textPrimary,
            fontFamily = ShareTechMono,
            fontSize = 22.sp,
            letterSpacing = 2.sp
        )
        Text(
            text = "Descargá el mapa vectorial de tu provincia para verlo sin conexión. " +
                   "Se guarda una sola vez en el teléfono.",
            color = colors.textSecondary,
            fontFamily = ShareTechMono,
            fontSize = 14.sp
        )

        manifestError?.let {
            Text(text = it, color = ar.motorfar.app.ui.compose.theme.EmergencyBorder, fontFamily = ShareTechMono, fontSize = 13.sp)
        }

        val list = provinces
        if (list == null && manifestError == null) {
            CircularProgressIndicator(color = colors.accent)
        }

        list?.let { infos ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(infos) { info ->
                    ProvinceRow(
                        info = info,
                        state = states[info.iso] ?: DownloadState.NotDownloaded,
                        onDownload = {
                            states = states + (info.iso to DownloadState.Downloading(0f))
                        },
                        onProgress = { progress ->
                            states = states + (info.iso to DownloadState.Downloading(progress))
                        },
                        onDone = {
                            states = states + (info.iso to DownloadState.Downloaded)
                        },
                        onError = { message ->
                            states = states + (info.iso to DownloadState.Failed(message))
                        },
                        onView = { viewingProvince = info },
                        destDir = destDir
                    )
                }
            }
        }
    }
}

@Composable
private fun ProvinceRow(
    info: ProvinceMapInfo,
    state: DownloadState,
    onDownload: () -> Unit,
    onProgress: (Float) -> Unit,
    onDone: () -> Unit,
    onError: (String) -> Unit,
    onView: () -> Unit,
    destDir: File
) {
    val colors = LocalMotoRFARColors.current
    val sizeMb = info.sizeBytes / (1024f * 1024f)

    LaunchedEffect(info.iso, state is DownloadState.Downloading) {
        if (state is DownloadState.Downloading && state.progress == 0f) {
            try {
                ProvinceMapRepository.downloadProvince(info, destDir) { progress -> onProgress(progress) }
                onDone()
            } catch (e: Exception) {
                onError(e.message ?: "Error al descargar")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.borderActive, RoundedCornerShape(4.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "${info.name}  ·  %.1f MB".format(sizeMb),
            color = colors.textPrimary,
            fontFamily = ShareTechMono,
            fontSize = 15.sp
        )
        when (val s = state) {
            is DownloadState.NotDownloaded -> {
                Button(onClick = onDownload) { Text("DESCARGAR", fontFamily = ShareTechMono) }
            }
            is DownloadState.Downloading -> {
                LinearProgressIndicator(
                    progress = { s.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.accent,
                    trackColor = colors.surface
                )
                Text(
                    text = "${(s.progress * 100).toInt()}%",
                    color = colors.textSecondary,
                    fontFamily = ShareTechMono,
                    fontSize = 12.sp
                )
            }
            is DownloadState.Downloaded -> {
                Button(onClick = onView) { Text("VER MAPA", fontFamily = ShareTechMono) }
            }
            is DownloadState.Failed -> {
                Text(
                    text = s.message,
                    color = ar.motorfar.app.ui.compose.theme.EmergencyBorder,
                    fontFamily = ShareTechMono,
                    fontSize = 12.sp
                )
                Button(onClick = onDownload) { Text("REINTENTAR", fontFamily = ShareTechMono) }
            }
        }
    }
}

/** Visor de solo lectura -- sin GPS/ruta/marcadores, solo pan/zoom del .map vectorial. */
@Composable
private fun MapsforgeViewerScreen(mapFile: File, title: String, onBack: () -> Unit) {
    val colors = LocalMotoRFARColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "‹ Volver ($title)",
            color = colors.textSecondary,
            fontFamily = ShareTechMono,
            fontSize = 15.sp,
            modifier = Modifier.padding(16.dp).clickable(onClick = onBack)
        )
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    ensureGraphicFactory(ctx)
                    val mapView = MapView(ctx).apply {
                        setBuiltInZoomControls(true)
                        mapZoomControls.isAutoHide = true
                    }
                    val tileCache = AndroidUtil.createTileCache(
                        ctx,
                        "offline_${title}",
                        mapView.model.displayModel.tileSize,
                        1f,
                        mapView.model.frameBufferModel.overdrawFactor
                    )
                    val dataStore = MapFile(mapFile)
                    val tileRendererLayer = AndroidUtil.createTileRendererLayer(
                        tileCache,
                        mapView.model.mapViewPosition,
                        dataStore,
                        MapsforgeThemes.DEFAULT,
                        false,
                        true,
                        false
                    )
                    mapView.layerManager.layers.add(tileRendererLayer)

                    val startPosition = dataStore.startPosition()
                    val zoom = dataStore.startZoomLevel() ?: 10
                    if (startPosition != null) {
                        mapView.model.mapViewPosition.setMapPosition(
                            org.mapsforge.core.model.MapPosition(startPosition, zoom)
                        )
                    } else {
                        val center = dataStore.boundingBox().centerPoint
                        mapView.model.mapViewPosition.setMapPosition(
                            org.mapsforge.core.model.MapPosition(center, zoom)
                        )
                    }
                    mapView
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose { AndroidGraphicFactory.clearResourceMemoryCache() }
    }
}
