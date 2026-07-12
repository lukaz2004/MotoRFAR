package ar.motorfar.app.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.data.RouteSessionSummary
import ar.motorfar.app.ui.compose.theme.EmergencyBorder
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.ShareTechMono
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Historial de salidas guardadas -- antes solo se veía/borraba la última
 * sesión de ruta; acá se listan todas, con opción de ver cada una en el
 * mapa o borrarla individualmente.
 */
@Composable
fun RouteHistoryScreen(
    sessions: List<RouteSessionSummary>,
    onViewSession: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onBack: () -> Unit = {}
) {
    val colors = LocalMotoRFARColors.current
    var pendingDelete by remember { mutableStateOf<Long?>(null) }
    val dateFmt = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text     = "‹ Volver",
            color    = colors.textSecondary,
            fontFamily = ShareTechMono,
            fontSize = 15.sp,
            modifier = Modifier.clickable(onClick = onBack)
        )

        Text(
            text     = "HISTORIAL DE RUTAS",
            color    = colors.textPrimary,
            fontFamily = ShareTechMono,
            fontSize = 22.sp,
            letterSpacing = 2.sp
        )

        if (sessions.isEmpty()) {
            Text(
                text     = "Todavía no hay salidas guardadas.",
                color    = colors.textSecondary,
                fontFamily = ShareTechMono,
                fontSize = 14.sp
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sessions, key = { it.sessionId }) { session ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.borderSubtle, RoundedCornerShape(4.dp))
                            .background(colors.surface, RoundedCornerShape(4.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text     = "${dateFmt.format(Date(session.startedAt))} — ${dateFmt.format(Date(session.endedAt))}",
                            color    = colors.textPrimary,
                            fontFamily = ShareTechMono,
                            fontSize = 15.sp
                        )
                        Text(
                            text     = "${session.pointCount} puntos guardados",
                            color    = colors.textSecondary,
                            fontFamily = ShareTechMono,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text     = "VER EN MAPA",
                                color    = colors.accent,
                                fontFamily = ShareTechMono,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { onViewSession(session.sessionId) }
                            )
                            Text(
                                text     = "BORRAR",
                                color    = EmergencyBorder,
                                fontFamily = ShareTechMono,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { pendingDelete = session.sessionId }
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { sessionId ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("¿Borrar esta salida?") },
            text  = { Text("Se elimina el recorrido guardado de esa salida. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSession(sessionId)
                    pendingDelete = null
                }) { Text("Borrar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            }
        )
    }
}
