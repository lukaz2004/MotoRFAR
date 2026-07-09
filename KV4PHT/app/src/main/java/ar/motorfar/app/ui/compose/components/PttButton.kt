package ar.motorfar.app.ui.compose.components

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.ShareTechMono

private const val RING_CYCLE_MS = 1200
private const val RING_COUNT    = 3

@Composable
fun PttButton(
    isTransmitting: Boolean,
    enabled: Boolean,
    onPttDown: () -> Unit,
    onPttUp: () -> Unit,
    diameter: Dp = 180.dp,
    // 2026-07-06: durante una transmisión de Emergencia el botón y sus
    // anillos pasan a rojo -- antes no había ninguna señal en el PTT de que
    // se estaba transmitiendo por el canal de emergencia.
    isEmergency: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors      = LocalMotoRFARColors.current
    val accentColor = when {
        isEmergency -> ar.motorfar.app.ui.compose.theme.EmergencyBorder
        enabled     -> colors.accent
        else        -> colors.textDisabled
    }
    val haptics     = LocalHapticFeedback.current
    val label       = if (isTransmitting) "TRANSMITIENDO" else "PUSH TO TALK"

    PttVisual(
        diameter       = diameter,
        accentColor    = accentColor,
        isTransmitting = isTransmitting,
        label          = label,
        modifier = modifier.pointerInput(enabled) {
            if (!enabled) return@pointerInput
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
 * Círculo con gradiente + anillos de TX + chip de texto, sin lógica de tap propia --
 * el caller decide cómo/cuándo dispara onPttDown/onPttUp (ver [PttButton] y el PTT
 * compacto de MapScreen, que necesita seguir avisando en modo escucha en vez de
 * bloquear el tap entero).
 */
@Composable
fun PttVisual(
    diameter: Dp,
    accentColor: Color,
    isTransmitting: Boolean,
    label: String,
    modifier: Modifier = Modifier
) {
    // Anillos radiales — solo activos durante TX
    val infiniteTransition = rememberInfiniteTransition(label = "ptt_rings")
    val ringPhase by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(RING_CYCLE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_phase"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(diameter)
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val center   = Offset(size.width / 2f, size.height / 2f)
            // ponytail: 0.78 en vez de 0.7 -- menos margen muerto alrededor del
            // círculo, dejando lo justo para que el anillo de TX (1.42x) no se
            // corte contra el borde del canvas.
            val baseRadius = size.minDimension / 2f * 0.78f

            // Anillos expansivos durante TX (contenidos dentro del canvas)
            if (isTransmitting) {
                for (i in 0 until RING_COUNT) {
                    val phase  = (ringPhase + i.toFloat() / RING_COUNT) % 1f
                    val radius = baseRadius * (1f + phase * 0.42f)
                    val alpha  = (1f - phase) * 0.55f
                    drawCircle(
                        color  = accentColor.copy(alpha = alpha),
                        radius = radius,
                        center = center,
                        style  = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // Botón principal
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.9f),
                        Color(0xFF101010)
                    ),
                    center = center,
                    radius = baseRadius
                ),
                radius = baseRadius
            )

            // Borde activo al transmitir
            if (isTransmitting) {
                drawCircle(
                    color  = accentColor,
                    radius = baseRadius - 2.dp.toPx(),
                    style  = Stroke(width = 3.dp.toPx())
                )
            }
        }
        // ponytail: el blanco desentonaba con la paleta CRT (verde/ámbar/rojo) --
        // en vez de un halo blanco, un chip oscuro fijo detrás del texto en el
        // color de acento; se lee igual en cualquier tema, sin desentonar.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text          = label,
                color         = accentColor,
                fontFamily    = ShareTechMono,
                fontSize      = 13.sp,
                fontWeight    = androidx.compose.ui.text.font.FontWeight.Bold,
                textAlign     = androidx.compose.ui.text.style.TextAlign.Center,
                letterSpacing = 1.sp
            )
        }
    }
}
