package ar.motorfar.app.ui.compose.components

import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.ui.compose.theme.BorderHairline
import ar.motorfar.app.ui.compose.theme.EmergencyBorder
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.PanelShape
import ar.motorfar.app.ui.compose.theme.ShareTechMono
import ar.motorfar.app.ui.compose.theme.crtScreen

@Composable
fun FrequencyDisplayCard(
    frequency: String,
    sMeterLevel: Int,
    // 2026-07-06: sin esto no hay forma de saber en que Hz estas sintonizado
    // desde la pantalla principal; se muestra al lado del modo.
    tone: String? = null,
    // Resalta en rojo cuando el canal activo es Emergencia -- antes el unico
    // indicador era el borde del botón del canal, poco visible manejando.
    isEmergencyActive: Boolean = false,
    // 2026-07-08: en horizontal la tarjeta comparte ancho con PTT y alertas --
    // el mismo tamaño de portrait (pensado a ancho completo) hacía que
    // "139.9700" se cortara en dos líneas. rememberUiScale() no alcanza para
    // esto porque escala con el ancho TOTAL de pantalla, no con la columna
    // real que le toca acá.
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalMotoRFARColors.current
    val emphasisColor = if (isEmergencyActive) EmergencyBorder else colors.textPrimary
    val uiScale = ar.motorfar.app.ui.compose.theme.rememberUiScale()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.display, PanelShape)
            .border(
                if (isEmergencyActive) 1.5.dp else BorderHairline,
                if (isEmergencyActive) EmergencyBorder else colors.borderSubtle,
                PanelShape
            )
            .clip(PanelShape)
            .crtScreen(colors)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ponytail: nombre de canal removido -- ya se ve resaltado en ChannelRow
        // debajo, mostrarlo acá arriba era redundante y quitaba una línea entera.
        Text(
            text  = frequency,
            color = emphasisColor,
            fontFamily = ShareTechMono,
            fontSize = ((if (compact) 24f else 36f) * uiScale).sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Text(
            text  = "MHz · FM" + (tone?.let { " · $it" } ?: ""),
            color = if (isEmergencyActive) EmergencyBorder else colors.textGhost,
            fontFamily = ShareTechMono,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(8.dp))
        SMeter(level = sMeterLevel)
    }
}

@Composable
fun SMeter(level: Int, modifier: Modifier = Modifier) {
    val colors = LocalMotoRFARColors.current
    val barCount = 9
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .padding(horizontal = 4.dp)
    ) {
        val barWidth  = (size.width - (barCount - 1) * 3f) / barCount
        for (i in 0 until barCount) {
            val barHeight = size.height * (0.4f + 0.6f * (i + 1) / barCount)
            val x = i * (barWidth + 3f)
            val alpha = if (i < level) 1f else 0.2f
            drawRect(
                color    = colors.accent.copy(alpha = alpha),
                topLeft  = Offset(x, size.height - barHeight),
                size     = Size(barWidth, barHeight)
            )
        }
    }
}
