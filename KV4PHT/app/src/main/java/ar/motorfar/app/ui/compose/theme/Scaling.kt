package ar.motorfar.app.ui.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Ancho de referencia (dp) en el que están pensados los tamaños fijos de la UI --
 * un teléfono moderno típico en portrait. Pantallas iguales o más anchas no
 * escalan (quedan exactamente como están, sin riesgo de regresión ahí).
 * Pantallas más angostas (Huawei P9: 360dp) escalan hacia abajo, con un piso
 * para que el texto nunca llegue a ilegible.
 */
private const val REFERENCE_WIDTH_DP = 400f
private const val MIN_SCALE = 0.75f

/** Factor de escala [MIN_SCALE, 1.0] según el ancho real de pantalla. */
@Composable
fun rememberUiScale(): Float {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return remember(widthDp) { (widthDp / REFERENCE_WIDTH_DP).coerceIn(MIN_SCALE, 1f) }
}
