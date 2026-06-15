package ar.motorfar.app.ui.compose.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import ar.motorfar.app.ui.compose.theme.BorderHairline
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.PanelShape
import ar.motorfar.app.ui.compose.theme.crtScreen
import kotlin.math.abs
import kotlin.math.sin

private const val BAR_COUNT = 24

/**
 * Visualizador de modulación estilo ecualizador de espectro.
 *
 * Dibuja barras verticales que suben y bajan de forma orgánica, sobre una
 * grilla tenue de osciloscopio (línea base + cuartos).
 * - Activo (isActive = true): barras animadas con amplitud completa — TX o RX.
 * - Inactivo: barras de baja amplitud (respiración), sutil.
 *
 * El color sigue el acento del tema (verde/ámbar CRT).
 */
@Composable
fun ModulationVisualizer(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalMotoRFARColors.current

    val transition = rememberInfiniteTransition(label = "modulation")
    val phase by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation  = tween(1400),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    // Segunda onda más lenta para que el patrón no se sienta repetitivo
    val phaseSlow by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation  = tween(2600),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_slow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .background(
                color = colors.surface.copy(alpha = 0.4f),
                shape = PanelShape
            )
            .border(
                width = BorderHairline,
                color = colors.borderSubtle,
                shape = PanelShape
            )
            .clip(PanelShape)
            .crtScreen(colors)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawSpectrum(
                phase     = phase,
                phaseSlow = phaseSlow,
                isActive  = isActive,
                barColor  = colors.accent,
                baseAlpha = if (isActive) 0.95f else 0.45f
            )
        }
    }
}

private fun DrawScope.drawSpectrum(
    phase: Float,
    phaseSlow: Float,
    isActive: Boolean,
    barColor: androidx.compose.ui.graphics.Color,
    baseAlpha: Float
) {
    val totalW   = size.width
    val gap      = 3.dp.toPx()
    val barW     = (totalW - gap * (BAR_COUNT - 1)) / BAR_COUNT
    val maxH     = size.height
    val centerY  = maxH / 2f

    // Grilla de osciloscopio: línea base central + cuartos, muy tenue
    val q = maxH / 4f
    drawLine(barColor.copy(alpha = 0.06f), Offset(0f, centerY - q), Offset(totalW, centerY - q), strokeWidth = 1f)
    drawLine(barColor.copy(alpha = 0.06f), Offset(0f, centerY + q), Offset(totalW, centerY + q), strokeWidth = 1f)
    drawLine(barColor.copy(alpha = 0.14f), Offset(0f, centerY),     Offset(totalW, centerY),     strokeWidth = 1f)

    for (i in 0 until BAR_COUNT) {
        // Amplitud combinando dos ondas + variación por índice para forma de espectro
        val t = i.toFloat() / BAR_COUNT
        val wave1 = sin(phase + i * 0.6f)
        val wave2 = sin(phaseSlow + i * 0.25f)
        // Envolvente tipo campana (barras del centro más altas)
        val envelope = 0.4f + 0.6f * sin(Math.PI.toFloat() * t)

        val amplitude = if (isActive) {
            (abs(wave1) * 0.6f + abs(wave2) * 0.4f) * envelope
        } else {
            // En reposo: ondas suaves visibles (respiración del espectro), no planas
            (0.25f + 0.18f * abs(wave1) + 0.12f * abs(wave2)) * envelope
        }

        val barH = (maxH * 0.92f) * amplitude
        val x    = i * (barW + gap)
        val top  = centerY - barH / 2f

        drawRoundRect(
            color        = barColor.copy(alpha = baseAlpha),
            topLeft      = Offset(x, top),
            size         = Size(barW, barH.coerceAtLeast(2.dp.toPx())),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 3f)
        )
    }
}
