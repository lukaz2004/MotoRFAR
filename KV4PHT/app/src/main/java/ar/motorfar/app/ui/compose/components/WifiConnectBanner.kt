package ar.motorfar.app.ui.compose.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.motorfar.app.R
import ar.motorfar.app.radio.WifiTransport
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.ShareTechMono

/**
 * Banner que aparece cuando la radio no está conectada (isConnected = false).
 * Informa al usuario que debe conectar el teléfono a la red WiFi del ESP32
 * y ofrece un atajo directo a los ajustes de WiFi del sistema.
 *
 * El nombre de red (SSID) viene de WifiTransport.AP_SSID para evitar literales duplicados.
 */
@Composable
fun WifiConnectBanner(
    modifier: Modifier = Modifier
) {
    val colors  = LocalMotoRFARColors.current
    val context = LocalContext.current

    // Pulso sutil en el ícono WiFi para llamar la atención sin ser molesto.
    val pulse by rememberInfiniteTransition(label = "wifi_pulse").animateFloat(
        initialValue  = 0.55f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wifi_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .border(1.dp, colors.accent.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter           = painterResource(R.drawable.ic_wifi),
                contentDescription = null,
                tint              = colors.accent.copy(alpha = pulse),
                modifier          = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text       = "SIN RADIO",
                color      = colors.accent,
                fontFamily = ShareTechMono,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Text(
            text       = "Conectate a la red WiFi del equipo:",
            color      = colors.textSecondary,
            fontFamily = ShareTechMono,
            fontSize   = 12.sp,
            modifier   = Modifier.padding(top = 6.dp, start = 28.dp)
        )
        Text(
            text       = WifiTransport.AP_SSID,
            color      = colors.textPrimary,
            fontFamily = ShareTechMono,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.padding(top = 2.dp, start = 28.dp)
        )

        Row(
            modifier            = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            ) {
                Text(
                    text       = "ABRIR AJUSTES WIFI  ›",
                    color      = colors.accent,
                    fontFamily = ShareTechMono,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** Versión compacta (una línea) para la barra superior cuando no hay espacio. */
@Composable
fun WifiConnectInline(modifier: Modifier = Modifier) {
    val colors  = LocalMotoRFARColors.current
    val context = LocalContext.current

    val pulse by rememberInfiniteTransition(label = "wifi_inline_pulse").animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label         = "wifi_inline_alpha"
    )

    Row(
        modifier          = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter            = painterResource(R.drawable.ic_wifi),
            contentDescription = null,
            tint               = Color(0xFFE24B4A).copy(alpha = pulse), // rojo pulsante = sin radio
            modifier           = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text       = "SIN RADIO · ${WifiTransport.AP_SSID}",
            color      = colors.textSecondary,
            fontFamily = ShareTechMono,
            fontSize   = 12.sp
        )
    }
}
