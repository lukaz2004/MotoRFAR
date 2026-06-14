package ar.motorfar.app.ui.compose.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Tokens de líneas y formas — fuente ÚNICA de verdad para todo el UI.
 *
 * Antes cada componente definía su propio grosor de borde y radio de esquina
 * (tarjeta de frecuencia cuadrada, visualizador 6dp, chips 4dp...), por lo que
 * "las líneas no coincidían". Centralizar acá garantiza coherencia visual.
 */

// Grosores de borde
val BorderHairline = 1.dp   // estado en reposo
val BorderStrong   = 2.dp   // estado activo / seleccionado

// Formas
val PanelShape   = RoundedCornerShape(6.dp)   // tarjetas y paneles (display, visualizador, banner)
val ControlShape = RoundedCornerShape(4.dp)   // chips y botones (canales, alertas)
