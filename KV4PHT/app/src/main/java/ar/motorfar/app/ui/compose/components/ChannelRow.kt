package ar.motorfar.app.ui.compose.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.data.ChannelMemory
import ar.motorfar.app.ui.compose.theme.BorderHairline
import ar.motorfar.app.ui.compose.theme.BorderStrong
import ar.motorfar.app.ui.compose.theme.ControlShape
import ar.motorfar.app.ui.compose.theme.EmergencyBorder
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.ShareTechMono

private const val EMERGENCY_FREQ = "140.970"

// Canales oficiales Res. M.T.T.T. 5/2015 — fallback si la DB aún no seedeó
private data class FixedChannel(val name: String, val freq: String)
private val FALLBACK_CHANNELS = listOf(
    FixedChannel("GRUPO",       "139.9700"),
    FixedChannel("ALTERNATIVO", "138.5100"),
    FixedChannel("EMERGENCIA",  "140.9700")
)

@Composable
fun ChannelRow(
    channels: List<ChannelMemory>,
    activeFreq: String,
    onChannelClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMotoRFARColors.current
    // Si la lista viene vacía (DB sin seed / sin radio), usa los 3 canales fijos
    val items: List<Pair<String, String>> = if (channels.isNotEmpty()) {
        channels.map { (it.name ?: it.frequency ?: "") to (it.frequency ?: "") }
    } else {
        FALLBACK_CHANNELS.map { it.name to it.freq }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { (name, freq) ->
            val isActive    = freq == activeFreq
            val isEmergency = freq.startsWith("140.97")
            val borderColor = when {
                isEmergency -> EmergencyBorder
                isActive    -> colors.borderActive
                else        -> colors.borderSubtle
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(colors.surface, ControlShape)
                    .border(if (isActive || isEmergency) BorderStrong else BorderHairline, borderColor, ControlShape)
                    .clickable { onChannelClick(freq) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text      = name,
                    color     = if (isEmergency) ar.motorfar.app.ui.compose.theme.EmergencyText
                                else if (isActive) colors.textPrimary
                                else colors.textSecondary,
                    fontFamily = ShareTechMono,
                    fontSize  = 14.sp,
                    textAlign = TextAlign.Center,
                    maxLines  = 1
                )
            }
        }
    }
}
