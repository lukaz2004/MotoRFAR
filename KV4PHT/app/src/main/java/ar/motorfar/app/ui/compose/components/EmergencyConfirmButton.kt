package ar.motorfar.app.ui.compose.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.ui.compose.theme.EmergencyBackground
import ar.motorfar.app.ui.compose.theme.EmergencyBorder
import ar.motorfar.app.ui.compose.theme.EmergencyText
import ar.motorfar.app.ui.compose.theme.ShareTechMono
import kotlinx.coroutines.delay

private const val CONFIRM_HOLD_MS = 2000L
private const val HOLD_STEP_MS    = 50L

/**
 * Botón de EMERGENCIA con confirmación por hold de 2 segundos.
 * Un toque corto no dispara nada. Solo el hold sostenido llama [onConfirmed].
 */
@Composable
fun EmergencyConfirmButton(
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isHolding by remember { mutableStateOf(false) }
    var progress  by remember { mutableFloatStateOf(0f) }
    var fired     by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue   = if (isHolding) 1f else 0f,
        animationSpec = tween(durationMillis = CONFIRM_HOLD_MS.toInt(), easing = LinearEasing),
        label         = "emergency_btn_arc"
    )

    LaunchedEffect(isHolding) {
        if (isHolding && !fired) {
            var elapsed = 0L
            while (elapsed < CONFIRM_HOLD_MS) {
                delay(HOLD_STEP_MS)
                elapsed += HOLD_STEP_MS
                progress = elapsed.toFloat() / CONFIRM_HOLD_MS.toFloat()
                if (!isHolding) break
            }
            if (isHolding && progress >= 1f) {
                fired = true
                onConfirmed()
            }
        } else {
            progress = 0f
            fired    = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        fired     = false
                        isHolding = true
                        tryAwaitRelease()
                        isHolding = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val r = CornerRadius(4.dp.toPx())  // = ControlShape, coincide con STOP/REAGRUPAR

            // Fondo base
            drawRoundRect(color = EmergencyBackground, cornerRadius = r)

            // Relleno de progreso (izquierda → derecha)
            val fillW = size.width * animatedProgress
            if (fillW > 0f) {
                drawRoundRect(
                    color        = EmergencyBorder.copy(alpha = 0.35f),
                    size         = Size(fillW, size.height),
                    cornerRadius = r
                )
            }

            // Borde — más brillante al sostener
            drawRoundRect(
                color        = if (isHolding) EmergencyBorder else EmergencyBorder.copy(alpha = 0.55f),
                cornerRadius = r,
                style        = Stroke(width = if (isHolding) 2.5.dp.toPx() else 1.5.dp.toPx())
            )
        }

        Text(
            text          = if (isHolding) "MANTENGA 2s..." else "⚠ EMERGENCIA",
            color         = EmergencyText,
            fontFamily    = ShareTechMono,
            fontSize      = 14.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
