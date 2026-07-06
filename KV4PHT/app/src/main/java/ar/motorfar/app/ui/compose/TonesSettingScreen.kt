package ar.motorfar.app.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.data.ChannelMemory
import ar.motorfar.app.ui.ToneHelper
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.ShareTechMono

/**
 * Selección de tono CTCSS por canal (Grupo/Alternativo). Emergencia se
 * muestra sin tono, a propósito — tiene que ser audible para cualquiera.
 */
@Composable
fun TonesSettingScreen(
    channels: List<ChannelMemory>,
    onToneSelected: (memoryId: Int, tone: String) -> Unit,
    onBack: () -> Unit = {}
) {
    val colors = LocalMotoRFARColors.current
    var showInfo by remember { mutableStateOf(false) }
    var editingChannel by remember { mutableStateOf<ChannelMemory?>(null) }

    val editable = channels.filter { it.name != "EMERGENCIA" }
    val emergencia = channels.firstOrNull { it.name == "EMERGENCIA" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text     = "‹ Volver",
                color    = colors.textSecondary,
                fontFamily = ShareTechMono,
                fontSize = 15.sp,
                modifier = Modifier.clickable(onClick = onBack)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text     = "TONOS CTCSS",
                color    = colors.textPrimary,
                fontFamily = ShareTechMono,
                fontSize = 22.sp,
                letterSpacing = 2.sp
            )
            Box(
                modifier = Modifier
                    .clickable { showInfo = true }
                    .border(1.dp, colors.borderActive, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "ⓘ ¿Qué es esto?",
                    color = colors.textPrimary,
                    fontFamily = ShareTechMono,
                    fontSize = 13.sp
                )
            }
        }

        editable.forEach { channel ->
            ChannelToneRow(channel = channel, onClick = { editingChannel = channel })
        }

        emergencia?.let {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface, RoundedCornerShape(4.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "EMERGENCIA — sin tono (fijo)",
                    color = colors.textSecondary,
                    fontFamily = ShareTechMono,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "No lleva tono a propósito: tiene que escucharla cualquiera en el canal, no solo tu grupo.",
                    color = colors.textSecondary,
                    fontFamily = ShareTechMono,
                    fontSize = 13.sp
                )
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("¿Qué es el tono CTCSS?") },
            text = {
                Text(
                    "Es como un \"código de silencio\" para tu grupo: tu radio se queda " +
                    "callado cuando escucha a otro grupo en el mismo canal con un tono " +
                    "distinto al tuyo — así no escuchás ni hablás sin querer con un grupo " +
                    "que no es el tuyo.\n\n" +
                    "Para que funcione, TODOS los integrantes de tu grupo tienen que estar " +
                    "en el MISMO canal (Grupo o Alternativo) Y con el MISMO tono. Si a uno " +
                    "le queda un canal o un tono distinto, ese queda afuera y no se escucha " +
                    "con el resto.\n\n" +
                    "IMPORTANTE — lo que el tono NO hace:\n" +
                    "• No hace privado el canal: cualquiera puede sacarse el filtro (es de " +
                    "fábrica en cualquier handy) y escucharte igual.\n" +
                    "• No evita que se pisen las transmisiones: si tu grupo y otro grupo " +
                    "hablan al mismo tiempo en el mismo canal, igual se interfieren entre " +
                    "sí, tengan el tono que tengan — el tono evita escuchar/hablar por error " +
                    "con otro grupo, no reparte el canal de radio en sí."
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) { Text("Entendido") }
            }
        )
    }

    editingChannel?.let { channel ->
        TonePickerDialog(
            currentTone = channel.txTone ?: "None",
            onDismiss = { editingChannel = null },
            onSelect = { tone ->
                onToneSelected(channel.memoryId, tone)
                editingChannel = null
            }
        )
    }
}

@Composable
private fun ChannelToneRow(channel: ChannelMemory, onClick: () -> Unit) {
    val colors = LocalMotoRFARColors.current
    val tone = ToneHelper.normalizeTone(channel.txTone ?: "None")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.borderActive, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = channel.name ?: "",
                color = colors.textPrimary,
                fontFamily = ShareTechMono,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (tone == "None") "Sin tono" else "$tone Hz",
                color = colors.textSecondary,
                fontFamily = ShareTechMono,
                fontSize = 14.sp
            )
        }
        Text(
            text = "CAMBIAR →",
            color = colors.accent,
            fontFamily = ShareTechMono,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun TonePickerDialog(
    currentTone: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val colors = LocalMotoRFARColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elegí el tono") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(ToneHelper.VALID_TONE_STRINGS) { tone ->
                    val label = if (tone == "None") "Sin tono" else "$tone Hz"
                    val isCurrent = tone == currentTone
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(tone) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            color = if (isCurrent) colors.accent else colors.textPrimary,
                            fontFamily = ShareTechMono,
                            fontSize = 16.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isCurrent) {
                            Text("✓", color = colors.accent, fontFamily = ShareTechMono, fontSize = 16.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
