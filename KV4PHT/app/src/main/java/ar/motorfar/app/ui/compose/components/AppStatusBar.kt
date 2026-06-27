package ar.motorfar.app.ui.compose.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.ShareTechMono

@Composable
fun AppStatusBar(
    isTx: Boolean,
    isRx: Boolean,
    isConnected: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = LocalMotoRFARColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "led_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue  = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "led_alpha"
    )
    val ledColor = when {
        !isConnected -> Color(0xFF666666).copy(alpha = pulse) // gris pulsante = buscando radio
        isTx         -> Color(0xFFE24B4A)                    // rojo = transmitiendo
        isRx         -> Color(0xFF4FBD3B).copy(alpha = pulse) // verde pulsante = recibiendo
        else         -> colors.accent.copy(alpha = 0.5f)      // acento tenue = listo/silencio
    }
    val statusLabel = when {
        !isConnected -> "SIN RADIO"
        isTx         -> "TX · VHF"
        isRx         -> "RX · VHF"
        else         -> "VHF · SIMPLEX"
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = ledColor)
        }
        Text(
            text       = statusLabel,
            color      = if (!isConnected) colors.textSecondary.copy(alpha = 0.6f)
                         else colors.textSecondary,
            fontFamily = ShareTechMono,
            fontSize   = 14.sp
        )
    }
}
