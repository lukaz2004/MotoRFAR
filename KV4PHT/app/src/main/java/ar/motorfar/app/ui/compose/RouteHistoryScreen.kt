package ar.motorfar.app.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.data.AppDatabase
import ar.motorfar.app.data.GpxExporter
import ar.motorfar.app.data.RoutePoint
import ar.motorfar.app.data.RouteSessionSummary
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.ShareTechMono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

private val DISPLAY_FMT = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
private val FILENAME_FMT = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US)

private fun distanceKm(points: List<RoutePoint>): Float {
    if (points.size < 2) return 0f
    var total = 0f
    val results = FloatArray(1)
    for (i in 1 until points.size) {
        android.location.Location.distanceBetween(
            points[i - 1].latitude, points[i - 1].longitude,
            points[i].latitude, points[i].longitude,
            results
        )
        total += results[0]
    }
    return total / 1000f
}

/** Historial de rutas por salida -- reemplaza el "borrar todo o nada" de antes. */
@Composable
fun RouteHistoryScreen(
    alias: String,
    onBack: () -> Unit = {},
    onViewSession: (sessionId: Long, points: List<RoutePoint>) -> Unit = { _, _ -> }
) {
    val colors = LocalMotoRFARColors.current
    val context = LocalContext.current

    var summaries by remember { mutableStateOf<List<RouteSessionSummary>>(emptyList()) }
    var distances by remember { mutableStateOf<Map<Long, Float>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var sessionPendingDelete by remember { mutableStateOf<Long?>(null) }
    var refreshTick by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTick) {
        loading = true
        withContext(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context.applicationContext).routePointDao()
            val loaded = dao.getSessionSummaries(alias)
            val dist = loaded.associate { s ->
                s.sessionId to distanceKm(dao.getPointsForSession(alias, s.sessionId))
            }
            summaries = loaded
            distances = dist
        }
        loading = false
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
            text = "HISTORIAL DE RUTAS",
            color = colors.textPrimary,
            fontFamily = ShareTechMono,
            fontSize = 22.sp,
            letterSpacing = 2.sp
        )

        if (loading) {
            CircularProgressIndicator(color = colors.accent)
        } else if (summaries.isEmpty()) {
            Text(
                text = "Todavía no guardaste ninguna salida.",
                color = colors.textSecondary,
                fontFamily = ShareTechMono,
                fontSize = 14.sp
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(summaries, key = { it.sessionId }) { summary ->
                    RouteSessionRow(
                        summary = summary,
                        distanceKm = distances[summary.sessionId] ?: 0f,
                        onView = {
                            val dao = AppDatabase.getInstance(context.applicationContext).routePointDao()
                            val points = dao.getPointsForSession(alias, summary.sessionId)
                            onViewSession(summary.sessionId, points)
                        },
                        onExport = {
                            val dao = AppDatabase.getInstance(context.applicationContext).routePointDao()
                            val points = dao.getPointsForSession(alias, summary.sessionId)
                            val label = "Ruta ${DISPLAY_FMT.format(java.util.Date(summary.startedAt))}"
                            val slug = "ruta_${FILENAME_FMT.format(java.util.Date(summary.startedAt))}"
                            GpxExporter.shareGpx(context, points, label, slug)
                        },
                        onDelete = { sessionPendingDelete = summary.sessionId }
                    )
                }
            }
        }
    }

    val pending = sessionPendingDelete
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { sessionPendingDelete = null },
            title = { Text("¿Borrar esta ruta?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    AppDatabase.getInstance(context.applicationContext).routePointDao()
                        .deleteSession(alias, pending)
                    sessionPendingDelete = null
                    refreshTick++
                }) { Text("Borrar") }
            },
            dismissButton = {
                TextButton(onClick = { sessionPendingDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun RouteSessionRow(
    summary: RouteSessionSummary,
    distanceKm: Float,
    onView: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalMotoRFARColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onView)
            .padding(12.dp)
    ) {
        Text(
            text = DISPLAY_FMT.format(java.util.Date(summary.startedAt)),
            color = colors.textPrimary,
            fontFamily = ShareTechMono,
            fontSize = 16.sp
        )
        Text(
            text = "%d puntos · %.1f km".format(summary.pointCount, distanceKm),
            color = colors.textSecondary,
            fontFamily = ShareTechMono,
            fontSize = 13.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onView) { Text("VER", fontFamily = ShareTechMono) }
            OutlinedButton(onClick = onExport) { Text("GPX", fontFamily = ShareTechMono) }
            OutlinedButton(
                onClick = onDelete,
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = ar.motorfar.app.ui.compose.theme.EmergencyBorder
                )
            ) { Text("BORRAR", fontFamily = ShareTechMono) }
        }
    }
}
