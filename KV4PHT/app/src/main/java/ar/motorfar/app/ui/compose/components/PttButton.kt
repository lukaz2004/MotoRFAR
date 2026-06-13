package ar.motorfar.app.ui.compose.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.ShareTechMono

@Composable
fun PttButton(
    isTransmitting: Boolean,
    enabled: Boolean,
    onPttDown: () -> Unit,
    onPttUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMotoRFARColors.current
    val accentColor = if (enabled) colors.accent else colors.textDisabled
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(90.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        onPttDown()
                        tryAwaitRelease()
                        onPttUp()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.size(90.dp)) {
            val radius = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.9f),
                        Color(0xFF101010)
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = radius
                ),
                radius = radius
            )
            // Borde activo al transmitir
            if (isTransmitting) {
                drawCircle(
                    color  = accentColor,
                    radius = radius - 2.dp.toPx(),
                    style  = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                )
            }
        }
        Text(
            text       = if (isTransmitting) "TX" else "PTT",
            color      = if (isTransmitting) Color.White else colors.background,
            fontFamily = ShareTechMono,
            fontSize   = 16.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}
