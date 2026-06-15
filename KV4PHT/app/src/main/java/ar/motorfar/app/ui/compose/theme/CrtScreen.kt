package ar.motorfar.app.ui.compose.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Overlay CRT sutil para los paneles "pantalla" (frecuencia, visualizador):
 * scanlines horizontales + viñeta de tubo en los bordes.
 *
 * Solo se aplica en temas oscuros — en el tema Día (claro) no hay CRT, es un
 * instrumento a la luz del día, así que se devuelve el modifier sin tocar.
 * La firma se gasta en un solo gesto y queda discreta (alpha bajo).
 */
fun Modifier.crtScreen(colors: MotoRFARColors, gap: Dp = 3.dp): Modifier {
    if (colors.background.luminance() >= 0.5f) return this   // tema claro: sin CRT
    val line = Color.Black.copy(alpha = 0.11f)
    return this.drawWithContent {
        drawContent()
        // Scanlines horizontales
        val g = gap.toPx().coerceAtLeast(2f)
        var y = 0f
        while (y < size.height) {
            drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += g
        }
        // Viñeta de tubo: bordes apenas más oscuros, centro limpio
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.16f)),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.maxDimension * 0.72f
            )
        )
    }
}
